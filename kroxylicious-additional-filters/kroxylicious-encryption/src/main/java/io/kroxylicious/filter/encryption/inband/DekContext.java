/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.inband;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A DekContext encapsulates an encryptor
 * @param <K> The type of the KEK id
 */
final class DekContext<K> {
    private final AesGcmEncryptor encryptor;
    private final ByteBuffer prefix;
    private final K kekId;

    DekContext(@NonNull K kekId,
               @NonNull ByteBuffer prefix,
               @NonNull AesGcmEncryptor encryptor) {
        this.kekId = kekId;
        this.prefix = prefix;
        this.encryptor = encryptor;
    }

    public @NonNull K kekId() {
        return kekId;
    }

    /**
     * Returns the size of the encoding of a plaintext of the given size
     * @param plaintextSize The plaintext.
     * @return The size, in bytes, of a plaintext.
     */
    public int encodedSize(int plaintextSize) {
        return prefix.capacity() + encryptor.outputSize(plaintextSize);
    }

    /**
     * Encode the key metadata and the ciphertext of the given {@code plaintext} to the given {@code output},
     * which should have at least {@link #encodedSize(int) encodedSize(plaintext)} bytes {@linkplain ByteBuffer#remaining() remaining}.
     * @param plaintext The plaintext
     * @param output The output buffer
     */
    public void encode(@NonNull ByteBuffer plaintext, @NonNull ByteBuffer output) {
        this.prefix.mark();
        output.put(this.prefix);
        this.prefix.reset();
        encryptor.encrypt(plaintext, output);
    }

}
