/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.service;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A ByteBuffer-sympathetic serializer for some type, {@code T}.
 * @param <T> The type of the serialized object.
 * @see De
 */
public interface Ser<T> {

    /**
     * Write a value in the range 0-255 as a single byte at the buffer's current {@link ByteBuffer#position()}.
     * The buffer's position will be incremented by 1.
     * @param buffer The buffer to write to
     * @param unsignedByte The value, which must be in the range [0, 255], which is not checked by this method.
     * @throws BufferOverflowException If this buffer's current position is not smaller than its limit.
     * @throws ReadOnlyBufferException If this buffer is read-only.
     * @see De#getUnsignedByte(ByteBuffer)
     */
    static void putUnsignedByte(@NonNull ByteBuffer buffer, int unsignedByte) {
        buffer.put((byte) (unsignedByte & 0xFF));

    }

    /**
     * Returns the number of bytes required to serialize the given object.
     * @param object The object to be serialized.
     * @return the number of bytes required to serialize the given object.
     */
    int sizeOf(T object);

    /**
     * Serializes the given object to the given buffer.
     * @param object The object to be serialized.
     * @param buffer the buffer to serialize the object to.
     * @throws BufferOverflowException If this buffer's current position is not smaller than its limit.
     * This should never be the case if the {@code buffer} has at least {@link #sizeOf(Object) sizeOf(object)} bytes remaining.
     * @throws ReadOnlyBufferException If this buffer is read-only.
     */
    void serialize(T object, @NonNull ByteBuffer buffer);

}
