/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson2;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.kroxylicious.filter.record.manipulation.common.Traversal;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A {@link Traversal} focusing on every element of an {@link ArrayNode}.
 */
public record ArrayItems() implements Traversal<ArrayNode, JsonNode> {

    @Override
    public List<JsonNode> getAll(ArrayNode array) {
        return array.valueStream().toList();
    }

    @Override
    public ArrayNode modifyAll(ArrayNode array, BaseTypedOp<JsonNode, JsonNode> f, OpContext opContext) {
        ArrayNode result = array.arrayNode(array.size());
        array.valueStream().map(node -> f.apply(node, opContext)).forEach(result::add);
        return result;
    }
}
