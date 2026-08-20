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

public class Jackson {
    private Jackson() {
    }

    public static Supplier<TextNode> convertString(Supplier<String> s) {
        return () -> new TextNode(s.get());
    }

    public static Function<JsonNode, TextNode> convertString(Function<String, String> fn) {
        return node -> new TextNode(fn.apply(node.asText()));
    }

    public static Supplier<IntNode> convertInt(IntSupplier s) {
        return () -> new IntNode(s.getAsInt());
    }

    static Supplier<LongNode> convertLong(LongSupplier s) {
        // TODO short, float, double, BigInteger, BigDecimal etc
        return () -> new LongNode(s.getAsLong());
    }
}
