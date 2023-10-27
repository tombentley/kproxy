/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.kafka.common.serialization.Serializer;

public interface Ser<T> {
    int sizeOf(T t);

    void serialize(T t, ByteBuffer buffer);

    static <T> Serializer<T> toKafka(Ser<T> ser) {
        return new Serializer<T>() {
            @Override
            public byte[] serialize(String topic, T data) {
                byte[] bytes = new byte[ser.sizeOf(data)];
                var buffer = ByteBuffer.wrap(bytes);
                ser.serialize(data, buffer);
                return bytes;
            }
        };
    }

    default <Y> Ser<T> then(Function<T, Y> then,
                            Ser<Y> y) {
        return new Ser<T>() {
            @Override
            public int sizeOf(T t) {
                return Ser.this.sizeOf(t) + y.sizeOf(then.apply(t));
            }

            @Override
            public void serialize(T t, ByteBuffer buffer) {
                Ser.this.serialize(t, buffer);
                y.serialize(then.apply(t), buffer);
            }
        };
    }
}
