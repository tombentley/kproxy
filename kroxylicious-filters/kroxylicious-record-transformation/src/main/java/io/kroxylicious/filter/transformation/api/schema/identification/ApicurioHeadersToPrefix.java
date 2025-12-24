/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

public class ApicurioHeadersToPrefix implements SchemaIdTransformation<ApicurioSchemaCoordinates, ByteWireId> {

    @Override
    public Class<ApicurioSchemaCoordinates> acceptedType() {
        return ApicurioSchemaCoordinates.class;
    }

    @Override
    public Class<ByteWireId> returnedType() {
        return ByteWireId.class;
    }

    @Override
    public ByteWireId schemaIdentifier(SchemaTransformationContext<ApicurioSchemaCoordinates> context) {
        long l = context.wireSchemaId().globalId();
        return new ByteWireId(l);
    }
}
