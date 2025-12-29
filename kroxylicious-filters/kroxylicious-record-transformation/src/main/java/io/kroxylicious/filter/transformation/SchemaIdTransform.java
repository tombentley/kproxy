/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.util.Objects;

import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.mapper.Mapper;
import io.kroxylicious.filter.transformation.api.schema.identification.OutputSchemaIdentification;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public record SchemaIdTransform<S extends WireSchemaId, W extends WireSchemaId>(
        Deserializer<S> inputSchemaIdentification,
        Mapper<S, W> schemaIdTransformation,
        OutputSchemaIdentification<W> outputschemaIdentification) {

    // TODO then implement the identity transformations for schema id.
    //  Because we'd expect that it's quite common to, for example
    //  have the identity data pipeline for keys or value, but not for schemas, or vice versa.
    //  And when the identity data pipeline is used then it would be common to use the
    //  Identity schema pipeline too

    // TODO allow schemaIdTransformation to have an async return type, that would allow looking up from a schema registry
    //   It might be possible to do that up-front, i.e. get all the input schema ids
    //   lookup the output schema ids
    //   load all the schemas
    //   transform the data

    public SchemaIdTransform {
        var deserializer = Objects.requireNonNull(inputSchemaIdentification);
        var mapper = Objects.requireNonNull(schemaIdTransformation);
        var serializer = Objects.requireNonNull(outputschemaIdentification);
        Class<? extends WireSchemaId> type = deserializer.returnedType();
        var typeSource = "the deserializer of type " + deserializer.getClass().getName();

        if (!mapper.acceptedType().isAssignableFrom(type)) {
            throw new TypeException("The mapper of type " + mapper.getClass().getName() + " cannot accept values of type " + type.getName() + " returned from " + typeSource);
        }
        type = mapper.returnedType();
        typeSource = "the mapper of type " + mapper.getClass().getName();

        if (!serializer.acceptedType().isAssignableFrom(type)) {
            throw new TypeException("The serializer of type " + serializer.getClass().getName() + " cannot accept values of type " + type.getName() + " returned from " + typeSource);
        }
    }
}
