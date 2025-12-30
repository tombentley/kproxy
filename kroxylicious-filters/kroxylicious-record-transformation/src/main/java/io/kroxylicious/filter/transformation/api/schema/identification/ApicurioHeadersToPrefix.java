/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.TypeException;

public class ApicurioHeadersToPrefix<S, T> implements SchemaIdMapping<S, T> {

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (type.wireSchemaId() != ApicurioSchemaCoordinates.class) {
            throw new TypeException("");
        }
        return new Type(ByteWireId.class, type.schema(), type.cls());
    }

    public WireSchemaId transformSchemaId(SchemaAndValue<S, T> schemaAndValue) {
        WireSchemaId byteWireId = new ByteWireId(((ApicurioSchemaCoordinates) schemaAndValue.schemaId()).globalId());
        return byteWireId;
    }
}
