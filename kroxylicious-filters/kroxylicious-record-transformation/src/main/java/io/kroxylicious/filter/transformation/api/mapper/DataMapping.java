/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public interface DataMapping<W extends WireSchemaId, S, V,
        W2 extends WireSchemaId, S2, V2>
        extends TypeCheckable {
    /**
     * Applies the transformation to the given value.
     *
     * @param value The value to be transformed.
     * @param context The context
     * @return The transformed value.
     */
    SchemaAndValue<W2, S2, V2> transform(SchemaAndValue<W, S, V> value, Context context);
}
