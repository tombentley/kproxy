/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.List;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.Traversal;

/**
 * A {@link Traversal} focusing on every element of an {@link ArrayNode}.
 */
public record ArrayItems() implements Traversal<ArrayNode, JsonNode> {

    @Override
    public List<JsonNode> getAll(ArrayNode array) {
        return array.valueStream().toList();
    }

    @Override
    public ArrayNode modifyAll(ArrayNode array, BiFunction<JsonNode, Context, JsonNode> f, Context context) {
        ArrayNode result = array.arrayNode(array.size());
        array.valueStream().map(node -> f.apply(node, context)).forEach(result::add);
        return result;
    }
}
