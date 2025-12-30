/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.avro;

import org.apache.avro.Schema;

import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.mapper.DataMapping;

public interface AvroDataMapping extends DataMapping<
        Schema, Object,
        Schema, Object> {

    @Override
    default Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (type.schema() == Schema.class) {
            throw new TypeException(String.format("%s requires input with schema type %s, "
                            + "but is being supplied with input with schema type %s",
                    AvroPatch.class.getName(),
                    Schema.class.getName(),
                    type.schema().getName()));
        }
        return new Type<>(type.wireSchemaId(), Schema.class, Object.class);
    }
}
