/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class Jackson2Tree implements TreeAdapter<JsonNode> {
    @Override
    public boolean isObject(JsonNode node) {
        return node.isObject();
    }

    @Override
    public boolean isArray(JsonNode node) {
        return node.isArray();
    }

    @Override
    public Iterable<? extends Map.Entry<String, JsonNode>> objectProperties(JsonNode node) {
        return (Iterable) node.properties();
    }

    @Override
    public int arrayLength(JsonNode node) {
        return node.size();
    }

    @Override
    public JsonNode arrayItem(JsonNode node, int index) {
        return node.get(index);
    }
}
