/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.inband;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.IntFunction;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.utils.ByteBufferOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

import io.kroxylicious.filter.encryption.DecryptionManager;
import io.kroxylicious.filter.encryption.EncryptionException;
import io.kroxylicious.filter.encryption.EncryptionManager;
import io.kroxylicious.filter.encryption.EncryptionVersion;
import io.kroxylicious.filter.encryption.FilterThreadExecutor;
import io.kroxylicious.filter.encryption.dek.CipherSpec;
import io.kroxylicious.filter.encryption.dek.Dek;
import io.kroxylicious.filter.encryption.dek.DekManager;
import io.kroxylicious.filter.encryption.records.RecordStream;
import io.kroxylicious.kms.service.Serde;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * An implementation of {@link EncryptionManager} and {@link DecryptionManager}
 * that uses envelope encryption, AES-GCM and stores the KEK id and encrypted DEK
 * alongside the record ("in-band").
 * @param <K> The type of KEK id.
 * @param <E> The type of the encrypted DEK.
 */
public class InBandDecryptionManager<K, E> implements DecryptionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(InBandDecryptionManager.class);

    public static final int NO_MAX_CACHE_SIZE = -1;

    private final AsyncLoadingCache<CacheKey<E>, Dek<E>> decryptorCache;
    private final DekManager<K, E> dekManager;
    private final FilterThreadExecutor filterThreadExecutor;

    private record CacheKey<E>(
                               @Nullable CipherSpec cipherSpec,
                               @Nullable E edek) {
        @SuppressWarnings("rawtypes")
        private static final CacheKey NONE = new CacheKey(null, null);

        @SuppressWarnings("unchecked")
        static <E> CacheKey<E> none() {
            return NONE;
        }

        public boolean isNone() {
            return cipherSpec == null || edek == null;
        }
    }

    public InBandDecryptionManager(DekManager<K, E> dekManager,
                                   @Nullable FilterThreadExecutor filterThreadExecutor,
                                   @Nullable Executor dekCacheExecutor,
                                   int dekCacheMaxItems) {
        this.dekManager = dekManager;
        this.filterThreadExecutor = filterThreadExecutor;
        Caffeine<Object, Object> cache = Caffeine.<CacheKey<E>, Dek<E>> newBuilder();
        if (dekCacheMaxItems != NO_MAX_CACHE_SIZE) {
            cache = cache.maximumSize(dekCacheMaxItems);
        }
        if (dekCacheExecutor != null) {
            cache = cache.executor(dekCacheExecutor);
        }
        // TODO support batched resolution in the DekManager and also in the KMS
        this.decryptorCache = cache
                .removalListener(this::afterCacheEviction)
                .buildAsync(this::loadDek);
    }

    CompletableFuture<Dek<E>> loadDek(CacheKey<E> cacheKey, Executor executor) {
        if (cacheKey == null || cacheKey.isNone()) {
            return CompletableFuture.completedFuture(null);
        }
        return dekManager.decryptEdek(cacheKey.edek(), cacheKey.cipherSpec())
                .toCompletableFuture();
    }

    /** Invoked by Caffeine after a DEK is evicted from the cache. */
    private void afterCacheEviction(@Nullable CacheKey<E> cacheKey,
                                    @Nullable Dek<E> dek,
                                    RemovalCause removalCause) {
        if (dek != null) {
            dek.destroyForDecrypt();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Attempted to destroy DEK: {}", dek);
            }
        }
    }

    /**
     * Reads the {@link RecordEncryptor#ENCRYPTION_HEADER_NAME} header from the record.
     * @param topicName The topic name.
     * @param partition The partition.
     * @param kafkaRecord The record.
     * @return The encryption header, or null if it's missing (indicating that the record wasn't encrypted).
     */
    static EncryptionVersion decryptionVersion(@NonNull String topicName,
                                               int partition,
                                               @NonNull Record kafkaRecord) {
        for (Header header : kafkaRecord.headers()) {
            if (RecordEncryptor.ENCRYPTION_HEADER_NAME.equals(header.key())) {
                byte[] value = header.value();
                if (value.length != 1) {
                    throw new EncryptionException("Invalid value for header with key '" + RecordEncryptor.ENCRYPTION_HEADER_NAME + "' "
                            + "in record at offset " + kafkaRecord.offset()
                            + " in partition " + partition
                            + " of topic " + topicName);
                }
                return EncryptionVersion.fromCode(value[0]);
            }
        }
        return null;
    }

    @NonNull
    @Override
    public CompletionStage<MemoryRecords> decrypt(@NonNull String topicName,
                                                  int partition,
                                                  @NonNull MemoryRecords records,
                                                  @NonNull IntFunction<ByteBufferOutputStream> bufferAllocator) {
        if (records.sizeInBytes() == 0) {
            // no records to transform, return input without modification
            return CompletableFuture.completedFuture(records);
        }
        List<Integer> batchRecordCounts = InBandEncryptionManager.batchRecordCounts(records);
        // it is possible to encounter MemoryRecords that have had all their records compacted away, but
        // the recordbatch metadata still exists. https://kafka.apache.org/documentation/#recordbatch
        if (batchRecordCounts.stream().allMatch(recordCount -> recordCount == 0)) {
            return CompletableFuture.completedFuture(records);
        }

        CompletionStage<List<DecryptState<E>>> decryptStates = resolveAll(topicName, partition, records);
        return decryptStates.thenApply(
                decryptStateList -> {
                    try {
                        return decrypt(topicName,
                                partition,
                                records,
                                decryptStateList,
                                allocateBufferForDecrypt(records, bufferAllocator));
                    }
                    finally {
                        for (var ds : decryptStateList) {
                            if (ds != null && ds.decryptor() != null) {
                                ds.decryptor().close();
                            }
                        }
                    }
                });
    }

    /**
     * Gets each record's encryption header (any any) and then resolves those edeks into
     * {@link io.kroxylicious.filter.encryption.dek.Dek.Decryptor}s via the {@link #dekManager}
     * @param topicName The topic name.
     * @param partition The partition.
     * @param records The records to decrypt.
     * @return A stage that completes with a list of the DecryptState
     * for each record in the given {@code records}, in the same order.
     */
    private CompletionStage<List<DecryptState<E>>> resolveAll(String topicName,
                                                              int partition,
                                                              MemoryRecords records) {
        Serde<E> serde = dekManager.edekSerde();
        var cacheKeys = new ArrayList<CacheKey<E>>();
        var states = new ArrayList<DecryptState<E>>();

        // Iterate the records collecting cache keys and decrypt states
        // both cacheKeys and decryptStates use the list index as a way of identifying the corresponding
        // record: The index in the list is the same as their index within the MemoryRecords
        RecordStream.ofRecords(records).forEachRecord((batch, record, ignored) -> {
            var decryptionVersion = decryptionVersion(topicName, partition, record);
            if (decryptionVersion != null) {
                ByteBuffer wrapper = record.value();
                // TODO it's ugly passing a BiFunction like CacheKey::new
                // should the wrapper just return a CacheKey directly
                cacheKeys.add(decryptionVersion.wrapperVersion().readSpecAndEdek(wrapper, serde, CacheKey::new));
                states.add(new DecryptState<>(decryptionVersion));
            }
            else {
                // It's not encrypted, so use sentinels
                cacheKeys.add(CacheKey.none());
                states.add(DecryptState.none());
            }
        });
        // Lookup the decryptors for the cache keys
        return filterThreadExecutor.completingOnFilterThread(decryptorCache.getAll(cacheKeys))
                .thenApply(cacheKeyDecryptorMap -> {
                    // Once we have the decryptors from the cache...
                    return issueDecryptors(cacheKeyDecryptorMap, cacheKeys, states);
                });
    }

    private @NonNull ArrayList<DecryptState<E>> issueDecryptors(@NonNull Map<CacheKey<E>, Dek<E>> cacheKeyDecryptorMap,
                                                                @NonNull ArrayList<CacheKey<E>> cacheKeys,
                                                                @NonNull ArrayList<DecryptState<E>> states) {
        Map<CacheKey<E>, Dek<E>.Decryptor> issuedDecryptors = new HashMap<>();
        try {
            for (int index = 0, cacheKeysSize = cacheKeys.size(); index < cacheKeysSize; index++) {
                CacheKey<E> cacheKey = cacheKeys.get(index);
                // ...update (in place) the DecryptState with the decryptor
                DecryptState<E> decryptState = states.get(index);
                var decryptor = issuedDecryptors.computeIfAbsent(cacheKey, k -> {
                    Dek<E> dek = cacheKeyDecryptorMap.get(cacheKey);
                    return dek != null ? dek.decryptor() : null;
                });
                decryptState.withDecryptor(decryptor);
            }
            // return the resolved DecryptStates
            return states;
        }
        catch (RuntimeException e) {
            issuedDecryptors.forEach((cacheKey, decryptor) -> decryptor.close());
            throw e;
        }
    }

    private static ByteBufferOutputStream allocateBufferForDecrypt(MemoryRecords memoryRecords,
                                                                   IntFunction<ByteBufferOutputStream> allocator) {
        int sizeEstimate = memoryRecords.sizeInBytes();
        return allocator.apply(sizeEstimate);
    }

    /**
     * Fill the given {@code buffer} with the {@code records},
     * decrypting any which are encrypted using the corresponding decryptor from the given
     * {@code decryptorList}.
     * @param records The records to decrypt.
     * @param decryptorList The decryptors to use.
     * @param buffer The buffer to fill (to encourage buffer reuse).
     * @return The decrypted records.
     */
    @NonNull
    private MemoryRecords decrypt(@NonNull String topicName,
                                  int parititon,
                                  @NonNull MemoryRecords records,
                                  @NonNull List<DecryptState<E>> decryptorList,
                                  @NonNull ByteBufferOutputStream buffer) {
        return RecordStream.ofRecordsWithIndex(records)
                .mapPerRecord((batch, record, index) -> decryptorList.get(index))
                .toMemoryRecords(buffer,
                        new RecordDecryptor<>(topicName, parititon));
    }
}
