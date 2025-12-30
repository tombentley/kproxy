/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.util.Optional;

import io.kroxylicious.filter.transformation.api.SchemaResolver;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.mapper.DataMapping;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdDeserializer;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

/**
 * A data transform where the schema information is known only after configuration time.
 * @param schemaIdDeserializer
 * @param schemaResolver
 * @param mapperOpt
 * @param serializer
 * @param <T>
 */
public record LateBoundDataTransform<W extends WireSchemaId, S, T>(
        SchemaIdDeserializer<W> schemaIdDeserializer,
        SchemaResolver schemaResolver,
        Optional<DataMapping<S, T, S, T>> mapperOpt,
        Serializer<T> serializer
) implements DataTransform {
    public LateBoundDataTransform {
        // TODO check that the schema resolve understands W
        // TODO we can infer from the mapper what S is, build a SchemaResolver which expects that, and fail if the schema is not of the right type
        Type<?, ?, ?> type = deserializer.typeCheck(Type.fromBytes());
        if (mapperOpt.isPresent()) {
            type = mapperOpt.get().typeCheck(type);
        }
        serializer.accepts(type);
    }
}
