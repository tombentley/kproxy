/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.model;

import java.io.IOException;
import java.util.Optional;

import org.apache.kafka.common.record.Record;

import io.kroxylicious.filter.transformation.api.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.mapper.DataMapping;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdSerializer;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdDeserializer;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

/**
 * Specifies a data transform where the schema information is known only after configuration time.
 */
public record LateBoundDataTransform<W extends WireSchemaId, S, V,
        W2 extends WireSchemaId, S2, V2>(
        SchemaIdDeserializer<W> schemaIdDeserializer,
        Optional<DataMapping<W, S, V, W2, S2, V2>> mapperOpt,
        Serializer<V2> serializer,
        SchemaIdSerializer<W2> schemaIdSerializer
) implements DataTransform {
//    public LateBoundDataTransform {
//        // TODO check that the schema resolve understands W
//        // TODO we can infer from the mapper what S is, build a SchemaResolver which expects that, and fail if the schema is not of the right type
//        Type<?, ?, ?> type = deserializer.typeCheck(Type.fromBytes());
//        if (mapperOpt.isPresent()) {
//            type = mapperOpt.get().typeCheck(type);
//        }
//        serializer.accepts(type);
//    }


}
