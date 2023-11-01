/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.service;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A ByteBuffer-sympathetic deserializer for some type, {@code T}.
 * @param <T> The type of the deserialized object.
 * @see Ser
 */
public interface De<T> {

    /**
     * Read a single byte from the buffer's {@link ByteBuffer#position() position} and return it as a value in the range 0-255.
     * The buffer's position will be incremented by 1.
     * @param buffer The buffer to read from.
     * @return The value, which will be in the range [0, 255].
     * @throws BufferUnderflowException If the buffer's current position is not smaller than its limit.
     * @see Ser#putUnsignedByte(ByteBuffer, int)
     */
    static short getUnsignedByte(@NonNull ByteBuffer buffer) {
        return (short) (buffer.get() & 0xFF);
    }

    /**
     * Deserialize an instance of {@code T} from the given buffer.
     * @param buffer The buffer.
     * @return The instance, which in general could be null.
     * @throws BufferUnderflowException If the buffer's current position is not smaller than its limit..
     */
    T deserialize(@NonNull ByteBuffer buffer);

}
