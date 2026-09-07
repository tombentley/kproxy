/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);

    @Test
    void convertStringGeneratorWrapsSuppliedValueInTextNode() {
        // Given
        var generator = Jackson.convertString(context -> "hello");

        // When
        TextNode node = generator.apply(OP_CONTEXT);

        // Then
        assertThat(node).isEqualTo(new TextNode("hello"));
    }

    @Test
    void convertStringFunctionAppliesFunctionToNodeTextAndWrapsResult() {
        // Given
        var function = Jackson.convertString((String s, OpContext context) -> s.toUpperCase());
        TextNode input = new TextNode("hello");

        // When
        TextNode result = function.apply(input, OP_CONTEXT);

        // Then
        assertThat(result).isEqualTo(new TextNode("HELLO"));
    }

    @Test
    void convertIntGeneratorWrapsSuppliedValueInIntNode() {
        // Given
        var generator = Jackson.convertInt(context -> 42);

        // When
        IntNode node = generator.apply(OP_CONTEXT);

        // Then
        assertThat(node).isEqualTo(new IntNode(42));
    }

    @Test
    void convertLongGeneratorWrapsSuppliedValueInLongNode() {
        // Given
        var generator = Jackson.convertLong(context -> 42L);

        // When
        LongNode node = generator.apply(OP_CONTEXT);

        // Then
        assertThat(node).isEqualTo(new LongNode(42L));
    }

}
