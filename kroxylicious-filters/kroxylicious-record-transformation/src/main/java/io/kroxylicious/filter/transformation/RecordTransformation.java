/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.util.List;
import java.util.Objects;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.mapper.Mapper;
import io.kroxylicious.filter.transformation.api.schema.identification.InputSchemaIdentification;
import io.kroxylicious.filter.transformation.api.schema.identification.OutputSchemaIdentification;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdTransformation;

/**
 * A collection of transformations to be applied to a record.
 */
public record RecordTransformation(

        Mapper<List<Header>, List<Header>> headerTransformation,

        // TODO factor out a schemaidpipeline and a datapipeline
        // TODO then implement (once!) the identity transformations for these things
        //  as a singleton, because we'd expect that it's quite common to, for example
        //  have the identity data pipeline for keys or value, but not the other
        //  And when the identity data pipeline is used then it would be common to use the
        //  Identity schema pipeline too

        InputSchemaIdentification keyInputSchemaIdentification,
        SchemaIdTransformation keySchemaIdTransformation,
        OutputSchemaIdentification keyOutputschemaIdentification,
        Deserializer<?> keyDeserializer,
        List<Mapper<?, ?>> keyMappers,
        Serializer<?> keySerializer,

        InputSchemaIdentification valueInputSchemaIdentification,
        SchemaIdTransformation valueSchemaIdTransformation,
        OutputSchemaIdentification valueOutputSchemaIdentification,
        Deserializer<?> valueDeserializer,
        List<Mapper<?, ?>> valueMappers,
        Serializer<?> valueSerializer
        ) {
    static void validatePipeline(Deserializer<?> deserializer,
                                 List<Mapper<?, ?>> mappers,
                                 Serializer<?> serializer) {
        Objects.requireNonNull(deserializer);
        Objects.requireNonNull(mappers);
        Objects.requireNonNull(serializer);
        var type = deserializer.returnedType();
        var typeSource = "the deserializer of type " + deserializer.getClass().getName();
        for (int i = 0; i < mappers.size(); i++) {
            Mapper<?, ?> mapper = mappers.get(i);
            if (!mapper.acceptedType().isAssignableFrom(type)) {
                throw new IllegalArgumentException("The mapper of type " + mapper.getClass().getName() + " cannot accept values of type " + type.getName() + " returned from " + typeSource);
            }
            type = mapper.returnedType();
            typeSource = "the mapper of type " + mapper.getClass().getName();
        }
        if (!serializer.acceptedType().isAssignableFrom(type)) {
            throw new IllegalArgumentException("The serializer of type " + serializer.getClass().getName() + " cannot accept values of type " + type.getName() + " returned from " + typeSource);
        }
    }

    public RecordTransformation {
        validatePipeline(keyDeserializer, keyMappers, keySerializer);
        validatePipeline(valueDeserializer, valueMappers, valueSerializer);
        // TODO need to validate the schema pipeline in the same way
    }
}
