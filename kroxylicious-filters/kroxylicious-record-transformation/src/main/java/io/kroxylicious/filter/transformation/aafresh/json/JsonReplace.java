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
 * <p>Replace a JSON node identified by a given JSONPointer.</p>
 * <p>Note that these semantics are different from
 * those of <a href="https://datatracker.ietf.org/doc/html/rfc6902">JSONPatch</a>,
 * where a non-match is an error</p>
 */
public class JsonReplace implements DataMapping2<JsonNode, JsonNode> {

    private final JsonPointer head;
    private final JsonPointer last;
    private final JsonNode replacement;

    JsonReplace(String pointer, @Nullable JsonNode replacement) {
        JsonPointer compile = JsonPointer.compile(pointer);
        this.head = compile.head();
        this.last = compile.last();
        this.replacement = replacement;
    }

    @Override
    public @Nullable JsonNode transform(@Nullable JsonNode root, Context context) {
        if (root != null) {
            JsonNode container = root.at(head);
            if (container.isArray() && last.mayMatchElement()) {
                ArrayNode arrayNode = (ArrayNode) container;
                int index = last.getMatchingIndex();
                if (index < arrayNode.size()) {
                    arrayNode.remove(index);
                    arrayNode.insert(index, replacement);
                }
                return root;
            }
            else if (container.isObject() && last.mayMatchProperty()) {
                ObjectNode objectNode = (ObjectNode) container;
                if (objectNode.has(last.getMatchingProperty())) {
                    objectNode.put(last.getMatchingProperty(), replacement);
                }
                else if (head.length() == 0 && last.length() == 1) {
                    return replacement;
                }
                return root;
            }
            else {
                return replacement;
            }
        }
        else {
            return root;
        }
    }

}
