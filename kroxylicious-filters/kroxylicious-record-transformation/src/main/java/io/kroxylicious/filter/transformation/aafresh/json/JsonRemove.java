/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh.json;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.kroxylicious.filter.transformation.aafresh.DataMapping2;
import io.kroxylicious.filter.transformation.api.mapper.Context;

import edu.umd.cs.findbugs.annotations.Nullable;

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
public class JsonRemove implements DataMapping2<JsonNode, JsonNode> {

    private final JsonPointer head;
    private final JsonPointer last;

    JsonRemove(String pointer) {
        JsonPointer compile = JsonPointer.compile(pointer);
        this.head = compile.head();
        this.last = compile.last();
    }

    @Override
    public @Nullable JsonNode transform(
            @Nullable JsonNode root,
            Context context) {
        if (root != null) {
            JsonNode container = root.at(head);
            if (last.mayMatchElement() && container.isArray()) {
                ArrayNode arrayNode = (ArrayNode) container;
                int index = last.getMatchingIndex();
                arrayNode.remove(index);
                return root;
            }
            else if (last.mayMatchProperty() && container.isObject()) {
                ObjectNode objectNode = (ObjectNode) container;
                objectNode.remove(last.getMatchingProperty());
                return root;
            }
            else {
                return root;
            }
        }
        else {
            return root;
        }
    }

}
