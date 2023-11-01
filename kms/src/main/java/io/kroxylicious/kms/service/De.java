/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.service;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A ByteBuffer-sympathetic deserializer for some type, {@code T}.
 * @param <T> The type of the deserialized object.
 * @see Ser
 */
public interface De<T> {

    /**
     * Deserialize an instance of {@code T} from the given buffer.
     * @param buffer The buffer.
     * @return The instance, which in general could be null.
     * @throws java.nio.BufferUnderflowException If the buffer didn't contain as many bytes as expected.
     */
    T deserialize(@NonNull ByteBuffer buffer);

}
