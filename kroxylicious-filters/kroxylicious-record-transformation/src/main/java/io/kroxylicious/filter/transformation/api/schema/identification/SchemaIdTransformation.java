/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import io.kroxylicious.filter.transformation.RecordDataLocation;

public interface SchemaIdTransformation<S extends WireSchemaId,
        W extends WireSchemaId> {

    static SchemaIdTransformation<WireSchemaId, WireSchemaId> preserve() {
        return new SchemaIdTransformation<>() {
            @Override
            public Class<WireSchemaId> acceptedType() {
                return WireSchemaId.class;
            }

            @Override
            public Class<WireSchemaId> returnedType() {
                return WireSchemaId.class;
            }

            @Override
            public WireSchemaId schemaIdentifier(SchemaTransformationContext<WireSchemaId> context) {
                return context.wireSchemaId();
            }
        };
    }

    static SchemaIdTransformation<WireSchemaId, NoSchema> noSchemaId() {
        return new SchemaIdTransformation<>() {
            @Override
            public Class<WireSchemaId> acceptedType() {
                return WireSchemaId.class;
            }

            @Override
            public Class<NoSchema> returnedType() {
                return NoSchema.class;
            }

            @Override
            public NoSchema schemaIdentifier(SchemaTransformationContext<WireSchemaId> context) {
                return NoSchema.INSTANCE;
            }
        };

    }

    record SchemaTransformationContext<S extends WireSchemaId>(String topicName,
                                       RecordDataLocation dataLocation,
                                       S wireSchemaId) {}

    Class<S> acceptedType();
    Class<W> returnedType();

    W schemaIdentifier(SchemaTransformationContext<S> context);

    // TODO allow this to have an async return type, that would allow looking up from a schema registry
    //   It might be possible to do that up-front, i.e. get all the input schema ids
    //   lookup the output schema ids
    //   load all the schemas
    //   transform the data
}


