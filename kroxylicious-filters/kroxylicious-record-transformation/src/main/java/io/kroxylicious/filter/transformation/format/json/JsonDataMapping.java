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
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaId;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public interface JsonDataMapping<W1 extends WireSchemaId, W2 extends WireSchemaId> extends DataMapping<W1, Object, JsonNode, W2, Void, JsonNode> {

    @Override
    default Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (!type.cls().isAssignableFrom(JsonNode.class)) {
            throw new TypeException(String.format("Type %s is not a JsonNode", type));
        }
        return new Type<>(NoSchemaId.class, Void.class, JsonNode.class);
    }
}
