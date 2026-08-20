/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonTest {

    @Test
    void convertStringSupplierWrapsSuppliedValueInTextNode() {
        // Given
        var supplier = Jackson.convertString(() -> "hello");

        // When
        TextNode node = supplier.get();

        // Then
        assertThat(node).isEqualTo(new TextNode("hello"));
    }

    @Test
    void convertStringFunctionAppliesFunctionToNodeTextAndWrapsResult() {
        // Given
        var function = Jackson.convertString(String::toUpperCase);
        TextNode input = new TextNode("hello");

        // When
        TextNode result = function.apply(input);

        // Then
        assertThat(result).isEqualTo(new TextNode("HELLO"));
    }

    @Test
    void convertIntSupplierWrapsSuppliedValueInIntNode() {
        // Given
        var supplier = Jackson.convertInt(() -> 42);

        // When
        IntNode node = supplier.get();

        // Then
        assertThat(node).isEqualTo(new IntNode(42));
    }

    @Test
    void convertLongSupplierWrapsSuppliedValueInLongNode() {
        // Given
        var supplier = Jackson.convertLong(() -> 42L);

        // When
        LongNode node = supplier.get();

        // Then
        assertThat(node).isEqualTo(new LongNode(42L));
    }

}
