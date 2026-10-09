/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.format.jackson2;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpConfigs;
import io.kroxylicious.transform.neo.op.OpContext;
import io.kroxylicious.transform.neo.ops.common.Traversal;

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
        array.valueStream().map(node -> OpConfigs.applyNow(f, node, opContext)).forEach(result::add);
        return result;
    }
}
