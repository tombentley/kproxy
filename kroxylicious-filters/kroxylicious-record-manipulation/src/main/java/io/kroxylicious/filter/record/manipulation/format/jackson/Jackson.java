/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * Adapters between the format-agnostic {@code common} primitives and Jackson's {@link JsonNode} types.
 */
public class Jackson {
    private Jackson() {
    }

    /**
     * Adapts a string generator.
     * @param fn the string generator to adapt
     * @return a function wrapping each generated string in a {@link TextNode}
     */
    public static Function<OpContext, TextNode> convertString(Function<OpContext, String> fn) {
        return context -> new TextNode(fn.apply(context));
    }

    /**
     * Adapts a string transformer.
     * @param fn the string transformer to adapt
     * @return a function that extracts the input node's text, applies {@code fn}, and wraps the result in a {@link TextNode}
     */
    public static BiFunction<JsonNode, OpContext, TextNode> convertString(BiFunction<String, OpContext, String> fn) {
        return (node, context) -> new TextNode(fn.apply(node.asText(), context));
    }

    /**
     * Adapts an int generator.
     * @param fn the int generator to adapt
     * @return a function wrapping each generated value in an {@link IntNode}
     */
    public static Function<OpContext, IntNode> convertInt(ToIntFunction<OpContext> fn) {
        return context -> new IntNode(fn.applyAsInt(context));
    }

    /**
     * Adapts a long generator.
     * @param fn the long generator to adapt
     * @return a function wrapping each generated value in a {@link LongNode}
     */
    static Function<OpContext, LongNode> convertLong(ToLongFunction<OpContext> fn) {
        // TODO short, float, double, BigInteger, BigDecimal etc
        return context -> new LongNode(fn.applyAsLong(context));
    }
}
