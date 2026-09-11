/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson3;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

public class DeserializeJsonTest {
    @Test
    void deser() {
        var op = new DeserializeJson().create(Map.of(), null, null);
        Object parsed = op.apply(ByteBuffer.wrap("{\"hello\": \"world\"}".getBytes(StandardCharsets.UTF_8)), null);
        assertThat(parsed).asInstanceOf(InstanceOfAssertFactories.type(ObjectNode.class)).
                extracting(JsonNode::propertyNames).isEqualTo(Set.of("hello"));
    }
}
