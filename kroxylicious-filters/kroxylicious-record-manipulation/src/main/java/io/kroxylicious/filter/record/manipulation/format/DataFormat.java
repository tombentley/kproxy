/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format;

/**
 * <p>A data format is a (serializer, deserialize)-pair with a common type argument (for type parameter {@code T}).</p>
 *
 * <p>Given
 * <pre>{@code
 * <T> boolean roundTrips(DataFormat<T> df, ByteBuffer in) {
 *   var deserializer = df.deserializer();
 *   var serializer = df.serializer();
 *   var out = serializer.serialize(deserializer.deserialize(in.duplicate()));
 *   return in.equals(out);
 * }
 * }</pre>
 * there is usually an expectation that {@code roundTrips} returns {@code true} for all {@code in} buffers which do not cause the {@code deserializer} to throw.
 * In other words, that everything that can be read can also be written, and the serialized form is bytewise identical.</p>
 * @param <T> The common Java type used to represent all data read using this data format.
 */
public interface DataFormat<T> {
    Serializer<T> serializer();

    Deserializer<T> deserializer();
}
