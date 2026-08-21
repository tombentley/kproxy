/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.kroxylicious.filter.record.manipulation.common.Context;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonTest {

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    @Test
    void convertStringGeneratorWrapsSuppliedValueInTextNode() {
        // Given
        var generator = Jackson.convertString(context -> "hello");

        // When
        TextNode node = generator.apply(CONTEXT);

        // Then
        assertThat(node).isEqualTo(new TextNode("hello"));
    }

    @Test
    void convertStringFunctionAppliesFunctionToNodeTextAndWrapsResult() {
        // Given
        var function = Jackson.convertString((String s, Context context) -> s.toUpperCase());
        TextNode input = new TextNode("hello");

        // When
        TextNode result = function.apply(input, CONTEXT);

        // Then
        assertThat(result).isEqualTo(new TextNode("HELLO"));
    }

    @Test
    void convertIntGeneratorWrapsSuppliedValueInIntNode() {
        // Given
        var generator = Jackson.convertInt(context -> 42);

        // When
        IntNode node = generator.apply(CONTEXT);

        // Then
        assertThat(node).isEqualTo(new IntNode(42));
    }

    @Test
    void convertLongGeneratorWrapsSuppliedValueInLongNode() {
        // Given
        var generator = Jackson.convertLong(context -> 42L);

        // When
        LongNode node = generator.apply(CONTEXT);

        // Then
        assertThat(node).isEqualTo(new LongNode(42L));
    }

}
