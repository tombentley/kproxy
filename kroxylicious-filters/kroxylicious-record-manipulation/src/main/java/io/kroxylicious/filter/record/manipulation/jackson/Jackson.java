/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Adapters between the format-agnostic {@code common} primitives and Jackson's {@link JsonNode} types.
 */
public class Jackson {
    private Jackson() {
    }

    /**
     * Adapts a string supplier.
     * @param s the string supplier to adapt
     * @return a supplier wrapping each supplied string in a {@link TextNode}
     */
    public static Supplier<TextNode> convertString(Supplier<String> s) {
        return () -> new TextNode(s.get());
    }

    /**
     * Adapts a string function.
     * @param fn the string function to adapt
     * @return a function that extracts the input node's text, applies {@code fn}, and wraps the result in a {@link TextNode}
     */
    public static Function<JsonNode, TextNode> convertString(Function<String, String> fn) {
        return node -> new TextNode(fn.apply(node.asText()));
    }

    /**
     * Adapts an int supplier.
     * @param s the int supplier to adapt
     * @return a supplier wrapping each supplied value in an {@link IntNode}
     */
    public static Supplier<IntNode> convertInt(IntSupplier s) {
        return () -> new IntNode(s.getAsInt());
    }

    /**
     * Adapts a long supplier.
     * @param s the long supplier to adapt
     * @return a supplier wrapping each supplied value in a {@link LongNode}
     */
    static Supplier<LongNode> convertLong(LongSupplier s) {
        // TODO short, float, double, BigInteger, BigDecimal etc
        return () -> new LongNode(s.getAsLong());
    }
}
