/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import org.apache.kafka.common.serialization.Deserializer;

import java.nio.ByteBuffer;

public interface De<T> {
    T deserialize(ByteBuffer buffer);

    static <T> Deserializer<T> toKafka(De<T> de) {
        return new Deserializer<T>() {
            @Override
            public T deserialize(String topic, byte[] data) {
                var buffer = ByteBuffer.wrap(data);
                return de.deserialize(buffer);
            }
        };
    }
}
