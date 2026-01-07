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

/**
 * <p>Remove a JSON node identified by a given JSONPointer.
 * This is a no-op if:</p>
 * <ul>
 * <li>the given JSONPointer identifies the root node.</li>
 * <li>the given JSONPointer identifies an array item which does not exist.</li>
 * <li>the given JSONPointer identifies an object entry which does not exist.</li>
 * </ul>
 * <p>Note that these semantics are different from those of <a href="https://datatracker.ietf.org/doc/html/rfc6902">JSONPatch</a>,
 * where a non-match is an error</p>
 */
public class JsonRemove<W extends WireSchemaId, S> implements DataMapping<
        W, S, JsonNode,
        W, S, JsonNode> {

    private final JsonPointer head;
    private final JsonPointer last;

    JsonRemove(String pointer) {
        JsonPointer compile = JsonPointer.compile(pointer);
        this.head = compile.head();
        this.last = compile.last();
    }

    @Override
    public SchemaAndValue<W, S, JsonNode> transform(
            SchemaAndValue<W, S, JsonNode> schemaAndValue,
            Context context) {
        JsonNode root = schemaAndValue.value();
        if (root != null) {
            JsonNode container = root.at(head);
            if (last.mayMatchElement() && container.isArray()) {
                ArrayNode arrayNode = (ArrayNode) container;
                int index = last.getMatchingIndex();
                arrayNode.remove(index);
                return schemaAndValue;
            }
            else if (last.mayMatchProperty() && container.isObject()) {
                ObjectNode objectNode = (ObjectNode) container;
                objectNode.remove(last.getMatchingProperty());
                return schemaAndValue;
            }
            else {
                return schemaAndValue;
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
