/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.Map;

import tools.jackson.databind.JsonNode;

public class Jackson3Tree implements TreeAdapter<JsonNode> {
    @Override
    public boolean isObject(JsonNode node) {
        return node.isObject();
    }

    @Override
    public boolean isArray(JsonNode node) {
        return node.isArray();
    }

    @Override
    public Iterable<? extends Prop<JsonNode>> objectProperties(JsonNode node) {
        return node.propertyStream().map(entry -> new Prop<>(entry.getKey(), entry.getValue())).toList();
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
