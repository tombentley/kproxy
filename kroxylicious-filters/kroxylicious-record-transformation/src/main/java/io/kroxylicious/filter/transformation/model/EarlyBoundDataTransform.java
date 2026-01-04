/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.model;

import java.util.Objects;
import java.util.Optional;

import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.format.DataFormat;
import io.kroxylicious.filter.transformation.api.mapper.DataMapping;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdSerializer;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdDeserializer;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

/**
 * A transformation on some data that doesn't depend on a schema (e.g. JSON)
 */
public record EarlyBoundDataTransform<W extends WireSchemaId, S, V,
        W2 extends WireSchemaId, S2, V2>(
        SchemaIdDeserializer<W> schemaIdDeserializer, // optional (e.g. there is a schema, but we don't need it to decode)
        DataFormat<S, V> dataFormat,
        Optional<DataMapping<W, S, V, W2, S2, V2>> mapperOpt,
        SchemaIdSerializer<W2> schemaIdSerializer
) implements DataTransform<W, S, V, W2, S2, V2> {

    public EarlyBoundDataTransform {
        Objects.requireNonNull(dataFormat);
        Objects.requireNonNull(mapperOpt);
        Objects.requireNonNull(dataFormat);

        var deserializer = dataFormat.deserializer(dataFormat.defaultEncoding());
        var serializer = dataFormat.serializer(dataFormat.defaultEncoding());

        Type<?, ?, ?> type = deserializer.typeCheck(Type.fromBytes());
        if (mapperOpt.isPresent()) {
            type = mapperOpt.get().typeCheck(type);
        }
        serializer.accepts(type);
    }
}
