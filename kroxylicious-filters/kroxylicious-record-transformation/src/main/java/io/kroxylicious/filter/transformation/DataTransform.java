/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.mapper.Mapper;
import io.kroxylicious.filter.transformation.api.mapper.Mappers;
import io.kroxylicious.filter.transformation.format.bytes.BytesDeserializer;
import io.kroxylicious.filter.transformation.format.bytes.BytesSerializer;

public record DataTransform(
        Deserializer<?> deserializer,
        Optional<Mapper<?, ?>> mapperOpt,
        Serializer<?> serializer
) {
    public static final DataTransform IDENTITY = new DataTransform(
            BytesDeserializer.INSTANCE,
            Optional.empty(),
            BytesSerializer.INSTANCE);

    public DataTransform(Deserializer<?> deserializer,
                         List<Mapper<?, ?>> mappers,
                         Serializer<?> serializer) {
        this(deserializer, mappers.isEmpty() ? Optional.empty() : Optional.of(Mappers.compose(mappers)), serializer);
    }

    public DataTransform {
        Objects.requireNonNull(deserializer);
        Objects.requireNonNull(mapperOpt);
        Objects.requireNonNull(serializer);
        var type = deserializer.returnedType();
        var typeSource = "the deserializer of type " + deserializer.getClass().getName();

        if (mapperOpt.isPresent()) {
            var mapper = mapperOpt.get();

            if (!mapper.acceptedType().isAssignableFrom(type)) {
                throw new IllegalArgumentException(
                        "The mapper of type " + mapper.getClass().getName() + " cannot accept values of type " + type.getName() + " returned from " + typeSource);
            }
            type = mapper.returnedType();
            typeSource = "the mapper of type " + mapper.getClass().getName();
        }

        if (!serializer.acceptedType().isAssignableFrom(type)) {
            throw new IllegalArgumentException("The serializer of type " + serializer.getClass().getName() + " cannot accept values of type " + type.getName() + " returned from " + typeSource);
        }
    }
}
