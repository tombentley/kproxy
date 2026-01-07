/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.json;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.DataMapping;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public class JsonReplace<W extends WireSchemaId, S> implements DataMapping<
        W, S, JsonNode,
        W, S, JsonNode> {

    private final JsonPointer head;
    private final JsonPointer last;
    private final JsonNode replacement;

    JsonReplace(String pointer, JsonNode replacement) {
        JsonPointer compile = JsonPointer.compile(pointer);
        this.head = compile.head();
        this.last = compile.last();
        this.replacement = replacement;
    }

    @Override
    public SchemaAndValue<W, S, JsonNode> transform(SchemaAndValue<W, S, JsonNode> schemaAndValue,
                                                                  Context context) {
        JsonNode root = schemaAndValue.value();
        if (root != null) {
            JsonNode container = root.at(head);
            if (container.isArray() && last.mayMatchElement()) {
                ArrayNode arrayNode = (ArrayNode) container;
                int index = last.getMatchingIndex();
                if (index < arrayNode.size()) {
                    arrayNode.remove(index);
                    arrayNode.insert(index, replacement);
                }
                return schemaAndValue;
            }
            else if (container.isObject() && last.mayMatchProperty()) {
                ObjectNode objectNode = (ObjectNode) container;
                if (objectNode.has(last.getMatchingProperty())) {
                    objectNode.put(last.getMatchingProperty(), replacement);
                }
                else if (head.length() == 0 && last.length() == 1) {
                    return new SchemaAndValue<>(schemaAndValue.schemaId(), schemaAndValue.schema(), replacement);
                }
                return schemaAndValue;
            }
            else {
                return new SchemaAndValue<>(schemaAndValue.schemaId(), schemaAndValue.schema(), replacement);
            }
        }
        else {
            return schemaAndValue;
        }
    }

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        return null;
    }
}
