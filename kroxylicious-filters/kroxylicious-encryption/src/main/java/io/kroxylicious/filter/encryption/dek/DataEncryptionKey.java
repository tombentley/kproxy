/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.dek;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kroxylicious.filter.encryption.EncryptionException;
import io.kroxylicious.filter.encryption.inband.ExhaustedDekException;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * An opaque handle on a key that can be used to encrypt and decrypt with some specific cipher.
 * @param <E> The type of encrypted DEK.
 */
@ThreadSafe
public final class DataEncryptionKey<E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataEncryptionKey.class);

    private static final Map<Class<? extends Destroyable>, Boolean> LOGGED_DESTROY_FAILED = new ConcurrentHashMap<>();

    private final E edek;
    // Note: checks for outstandingCryptors==END <em>happens-before</em> changes to atomicKey.
    // It's this that provides a guarantee that a cryptor should never see a
    // destroyed or null key, but that keys should be destroyed and nullieifed as soon
    // as possible once all outstanding cryptors are closed.
    private final AtomicReference<SecretKey> atomicKey;

    private final AtomicInteger remainingEncryptions;

    // 1 start
    // +1=2 (en|de)cryptor
    // +1=3 (en|de)cryptor
    // +1=4 (en|de)cryptor
    // -1=3 cryptor.finish // return
    // negate=-3 dek.destroy()// updateAndGet(if current value < 0 don't change, otherwise negate)
    // +1=-2 (en|de)cryptor.close
    // +1=-1 (en|de)cryptor.close => key.destroy
    final AtomicInteger outstandingCryptors;

    public static final int START = 1;
    public static final int END = -1;
    private final CipherSpec cipherSpec;

    static int acquireCryptor(int cryptors) {
        if (cryptors > 0) {
            return cryptors + 1;
        }
        else {
            return cryptors;
        }
    }

    static int releaseCryptor(int cryptors) {
        if (cryptors > 0) {
            return cryptors - 1;
        }
        else {
            return cryptors + 1;
        }
    }

    static int commenceDestroy(int combined) {
        int cryptors = combined;

        if (cryptors < 0) {
            return combined;
        }
        return -cryptors;
    }

    DataEncryptionKey(@NonNull E edek, @NonNull SecretKey key, @NonNull CipherSpec cipherSpec, int maxEncryptions) {
        /* protected access because instantion only allowed via a DekManager */
        Objects.requireNonNull(edek);
        if (Objects.requireNonNull(key).isDestroyed()) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(cipherSpec);
        if (maxEncryptions < 0) {
            throw new IllegalArgumentException();
        }
        this.edek = edek;
        this.atomicKey = new AtomicReference<>(key);
        this.cipherSpec = cipherSpec;
        this.remainingEncryptions = new AtomicInteger(maxEncryptions);
        this.outstandingCryptors = new AtomicInteger(START);
    }

    /**
     * Get an encryptor, good for at most {@code numEncryptions} {@linkplain Encryptor#encrypt(ByteBuffer, ByteBuffer, IntFunction, BiFunction) encryptions}.
     * The caller must invoke {@link Encryptor#close()} after performing all the required operations.
     * Note that while this method is safe to call from multiple threads, the returned encryptor is not.
     * @param numEncryptions The number of encryption operations required
     * @return The encryptor.
     * @throws IllegalArgumentException If {@code numEncryptions} is less and or equal to zero.
     * @throws ExhaustedDekException If the DEK cannot support the given number of encryptions
     * @throws DestroyedDekException If the DEK has been {@linkplain #destroy()} destroyed.
     */
    public @NonNull Encryptor encryptor(int numEncryptions) {
        if (numEncryptions <= 0) {
            throw new IllegalArgumentException();
        }
        if (remainingEncryptions.addAndGet(-numEncryptions) >= 0) {

            // TODO think about the possibilty of races between the key being set to null
            // and the decrementing of outstandingCryptors
            // We need to guarantee that NPE is not possible
            // The alternative is to make DEK#key final (not volatile) and give up on nullifying it
            // as part of destruction
            if (outstandingCryptors.updateAndGet(DataEncryptionKey::acquireCryptor) <= 0) {
                throw new DestroyedDekException();
            }
            return new Encryptor(cipherSpec, atomicKey.get(), numEncryptions);
        }
        throw new ExhaustedDekException("");
    }

    /**
     * Get a decryptor for this DEK.
     * Note that while this method is safe to call from multiple threads, the returned decryptor is not.
     * @return The decryptor.
     * @throws DestroyedDekException If the DEK has been {@linkplain #destroy()} destroyed.
     */
    public Decryptor decryptor() {
        if (outstandingCryptors.updateAndGet(DataEncryptionKey::acquireCryptor) <= 0) {
            throw new DestroyedDekException();
        }
        return new Decryptor(cipherSpec, atomicKey.get());
    }

    /**
     * Destroy the key.
     * The key is not destroyed immediately.
     * Calls to {@link #encryptor(int)} and {@link #decryptor()} will start to throw {@link DestroyedDekException},
     * but existing {@link Encryptor} and {@link Decryptor} instances will be able to continue.
     * The key will actually be destroyed once {@link Encryptor#close()} has been called on all existing Encryptors
     * and {@link Decryptor#close()} has been called on all existing Decryptors.
     * This method is idempotent.
     */
    public void destroy() {
        maybeDestroyKey(DataEncryptionKey::commenceDestroy);
    }

    private void maybeDestroyKey(IntUnaryOperator updateFunction) {
        if (outstandingCryptors.updateAndGet(updateFunction) == END) {
            var key = atomicKey.getAndSet(null);
            if (key != null) {
                try {
                    key.destroy();
                }
                catch (DestroyFailedException e) {
                    var cls = key.getClass();
                    if (LOGGER.isWarnEnabled()) {
                        LOGGED_DESTROY_FAILED.computeIfAbsent(cls, (c) -> {
                            LOGGER.warn("Failed to destroy an instance of {}. "
                                    + "Note: this message is logged once per class even though there may be many occurrences of this event. "
                                    + "This event can happen because the JRE's SecretKeySpec class does not override the destroy() method.",
                                    c, e);
                            return Boolean.TRUE;
                        });
                    }
                }
            }
        }
    }

    public boolean isDestroyed() {
        SecretKey secretKey = atomicKey.get();
        return secretKey == null || secretKey.isDestroyed();
    }

    /**
     * A means of performing a limited number of encryption operations without access to key material.
     */
    @NotThreadSafe
    public final class Encryptor implements AutoCloseable {
        private final Cipher cipher;
        private SecretKey key;
        private final Supplier<AlgorithmParameterSpec> paramSupplier;
        private final CipherSpec cipherSpec;
        private int numEncryptions;

        private Encryptor(CipherSpec cipherSpec, SecretKey key, int numEncryptions) {
            if (numEncryptions <= 0) {
                throw new IllegalArgumentException();
            }
            this.cipher = cipherSpec.newCipher();
            this.paramSupplier = cipherSpec.paramSupplier();
            this.cipherSpec = cipherSpec;
            this.key = key;
            this.numEncryptions = numEncryptions;
        }

        /**
         * @return The encrypted key
         */
        public E edek() {
            return edek;
        }

        /**
         * Perform an encryption operation using the DEK.
         * @param plaintext The plaintext to be encrypted
         * @param aad The AAD to be included in the encryption
         * @param paramAllocator A function that will return a buffer into which the cipher parameters will be written.
         * The function's argument is the number of bytes required for the cipher parameters.
         * @param ciphertextAllocator A function that will return a buffer into which the ciphertext will be written.
         * The function's first argument is the number of bytes required for the cipher parameters.
         * The function's second argument is the number of bytes required for the ciphertext.
         * @throws DekUsageException If this Encryptor has run out of operations.
         */
        public void encrypt(@NonNull ByteBuffer plaintext,
                            @Nullable ByteBuffer aad,
                            @NonNull IntFunction<ByteBuffer> paramAllocator,
                            @NonNull BiFunction<Integer, Integer, ByteBuffer> ciphertextAllocator) {

            if (--numEncryptions >= 0) {
                try {
                    AlgorithmParameterSpec params = paramSupplier.get();
                    cipher.init(Cipher.ENCRYPT_MODE, key, params);

                    int size = cipherSpec.size(params);
                    var parametersBuffer = paramAllocator.apply(size);
                    cipherSpec.writeParameters(parametersBuffer, params);
                    parametersBuffer.flip();

                    int outSize = cipher.getOutputSize(plaintext.remaining());
                    var ciphertext = ciphertextAllocator.apply(size, outSize);
                    if (aad != null) {
                        cipher.updateAAD(aad);
                        aad.rewind();
                    }
                    var p = plaintext.position();
                    cipher.doFinal(plaintext, ciphertext);
                    plaintext.position(p);
                    ciphertext.flip();

                    if (numEncryptions == 0) {
                        close();
                    }
                    return;
                }
                catch (GeneralSecurityException e) {
                    throw new EncryptionException(e);
                }
            }
            throw new DekUsageException("The Encryptor has no more operations allowed");
        }

        @Override
        public void close() {
            if (key != null) {
                key = null;
                maybeDestroyKey(DataEncryptionKey::releaseCryptor);
            }
        }
    }

    /**
     * A means of performing decryption operations without access to key material.
     */
    @NotThreadSafe
    public final class Decryptor implements AutoCloseable {
        private final Cipher cipher;
        private SecretKey key;
        private final CipherSpec cipherSpec;

        private Decryptor(CipherSpec cipherSpec, SecretKey key) {
            this.cipher = cipherSpec.newCipher();
            this.cipherSpec = cipherSpec;
            this.key = key;
        }

        /**
         * Perform an encryption operation using the DEK.
         * @param ciphertext The ciphertext.
         * @param aad The AAD.
         * @param parameterBuffer The buffer containing the cipher parameters.
         * @param plaintext The plaintext.
         */
        public void decrypt(@NonNull ByteBuffer ciphertext,
                            @Nullable ByteBuffer aad,
                            @NonNull ByteBuffer parameterBuffer,
                            @NonNull ByteBuffer plaintext) {
            try {
                var parameterSpec = cipherSpec.readParameters(parameterBuffer);
                cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
                if (aad != null) {
                    cipher.updateAAD(aad);
                }
                cipher.doFinal(ciphertext, plaintext);
            }
            catch (GeneralSecurityException e) {
                throw new EncryptionException(e);
            }
        }

        @Override
        public void close() {
            if (key != null) {
                key = null;
                maybeDestroyKey(DataEncryptionKey::releaseCryptor);
            }
        }
    }
}
