/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.service;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A ByteBuffer-sympathetic serializer for some type, {@code T}.
 * @param <T> The type of the serialized object.
 * @see De
 */
public interface Ser<T> {
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
     * @throws java.nio.BufferOverflowException if the given {@code buffer} does not have
     * sufficient remaining space to serialize the given {@code object}.
     * This should never be the case if the {@code buffer} has at least {@link #sizeOf(Object) sizeOf(object)} bytes remaining.
     */
    void serialize(T object, @NonNull ByteBuffer buffer);

}
