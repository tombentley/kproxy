/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import io.kroxylicious.filter.transformation.api.mapper.Context;

public class ApicurioHeadersToPrefix implements SchemaIdMapper<ApicurioSchemaCoordinates, ByteWireId> {

    @Override
    public Class<ApicurioSchemaCoordinates> acceptedType() {
        return ApicurioSchemaCoordinates.class;
    }

    @Override
    public Class<ByteWireId> returnedType() {
        return ByteWireId.class;
    }

    @Override
    public ByteWireId transform(ApicurioSchemaCoordinates wireSchemaId, Context context) {
        byte[] l = wireSchemaId.globalId();
        return new ByteWireId(l);
    }
}
