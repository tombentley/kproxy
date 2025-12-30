/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.TypeCheckable;

public interface SchemaIdMapping<S, T> extends TypeCheckable {

    default SchemaAndValue<S, T> transform(SchemaAndValue<S, T> schemaAndValue,
                                           Context context) {
        WireSchemaId wireSchemaId = transformSchemaId(schemaAndValue);
        return new SchemaAndValue<>(wireSchemaId, schemaAndValue.schema(), schemaAndValue.value());
    }

    WireSchemaId transformSchemaId(SchemaAndValue<S, T> schemaAndValue);

}


