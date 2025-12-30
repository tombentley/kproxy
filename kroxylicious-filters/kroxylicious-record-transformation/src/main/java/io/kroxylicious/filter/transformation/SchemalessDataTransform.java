/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.mapper.DataMapping;
import io.kroxylicious.filter.transformation.api.mapper.Mappers;
import io.kroxylicious.filter.transformation.format.bytes.BytesDeserializer;
import io.kroxylicious.filter.transformation.format.bytes.BytesSerializer;

/**
 * A transformation on some data that doesn't depend on a schema (e.g. JSON)
 * @param deserializer
 * @param mapperOpt
 * @param serializer
 * @param <T>
 */
public record SchemalessDataTransform<T>(
        Deserializer<Void, T> deserializer,
        Optional<DataMapping<Void, T, Void, T>> mapperOpt,
        Serializer<T> serializer
) implements DataTransform {
    public static final SchemalessDataTransform IDENTITY = new SchemalessDataTransform(
            BytesDeserializer.INSTANCE,
            Optional.empty(),
            BytesSerializer.INSTANCE);

    public SchemalessDataTransform(Deserializer<Void, T> deserializer,
                                   List<DataMapping<Void, T, Void, T>> mappers,
                                   Serializer<T> serializer) {
        this(deserializer, Optional.of(Mappers.compose((List) mappers)), serializer);
    }

    public SchemalessDataTransform {
        Objects.requireNonNull(deserializer);
        Objects.requireNonNull(mapperOpt);
        Objects.requireNonNull(serializer);

        Type<?, ?, ?> type = deserializer.typeCheck(Type.fromBytes());
        if (mapperOpt.isPresent()) {
            type = mapperOpt.get().typeCheck(type);
        }
        serializer.accepts(type);
    }
}
