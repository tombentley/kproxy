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
     * @param t The object to be serialized.
     * @return the number of bytes required to serialize the given object.
     */
    int sizeOf(T t);

    /**
     * Serializes the given object to the given buffer.
     * @param t The object to be serialized.
     * @param buffer the buffer to serialize the object to.
     */
    void serialize(T t, @NonNull ByteBuffer buffer);

}
