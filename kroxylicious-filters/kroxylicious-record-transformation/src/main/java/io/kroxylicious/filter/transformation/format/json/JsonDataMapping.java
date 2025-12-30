/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.json;

import com.fasterxml.jackson.databind.JsonNode;

import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.mapper.DataMapping;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchema;

public interface JsonDataMapping extends DataMapping<Object, JsonNode, Void, JsonNode> {

    @Override
    default Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (!type.cls().isAssignableFrom(JsonNode.class)) {
            throw new TypeException(String.format("Type %s is not a JsonNode", type));
        }
        return new Type<>(NoSchema.class, Void.class, JsonNode.class);
    }
}
