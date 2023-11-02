/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import edu.umd.cs.findbugs.annotations.NonNull;

import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.MemoryRecordsBuilder;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface DekCache<K, E> {

    // TODO define contract for the DekCache attempting to destroy key material when it's no longer being used

    /**
     * Asynchronously gets the current DEK context for the Key Encryption Key with the given {@code kekId}.
     * @param kekId The KEK ids
     * @return The DEK context for this key
     */
    @NonNull CompletionStage<Void> forKekId(@NonNull K kekId, Stream<PartitionEncryptionRequest> x,
                                          @NonNull Receiver consumer,
                                          @NonNull BiConsumer<PartitionEncryptionRequest, MemoryRecords> fooble);


    /**
     * Asynchronously resolves the DEK context from (a prefix of) the given {@code buffer}.
     * Following a successful call the given {@code buffer}'s position should at the first byte following the DEK
     * @param buffer The buffer.
     * @return The DEK context for the given buffer.
     */
    @NonNull CompletionStage<AesGcmEncryptor> resolve(@NonNull ByteBuffer buffer);
}
