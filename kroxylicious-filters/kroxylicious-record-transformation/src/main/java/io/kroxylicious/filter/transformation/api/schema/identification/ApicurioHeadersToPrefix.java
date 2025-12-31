/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.DataMapping;

public class ApicurioHeadersToPrefix<S, T> implements DataMapping<ApicurioSchemaCoordinates, S, T, ByteWireId, S, T> {


    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (type.wireSchemaId() != ApicurioSchemaCoordinates.class) {
            throw new TypeException("");
        }
        return new Type<>(ByteWireId.class, type.schema(), type.cls());
    }

    @Override
    public SchemaAndValue<ByteWireId, S, T> transform(SchemaAndValue<ApicurioSchemaCoordinates, S, T> schemaAndValue,
                                           Context context) {
        return schemaAndValue.withSchemaId(new ByteWireId(schemaAndValue.schemaId().globalId()));
    }

}
