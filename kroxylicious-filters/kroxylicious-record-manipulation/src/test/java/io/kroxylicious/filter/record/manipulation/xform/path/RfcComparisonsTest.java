/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.kroxylicious.filter.record.manipulation.type.Nothing;
import io.kroxylicious.filter.record.manipulation.type.Null;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.MissingNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link RfcComparisons} implements RFC 9535 §2.3.5.2 comparison semantics, extended with this
 * module's {@link Nothing} (absence) and {@link Null} (present null) values, and that it never throws on a
 * type mismatch.
 */
class RfcComparisonsTest {

    private static final JsonNodeFactory NODES = new JsonNodeFactory();

    static Stream<Arguments> equality() {
        return Stream.of(
                // numbers compare by value, across representations
                Arguments.of(1L, 1L, true),
                Arguments.of(1L, 2L, false),
                Arguments.of(1L, 1.0d, true),
                Arguments.of(1L, NODES.numberNode(1), true),
                Arguments.of(NODES.numberNode(1), NODES.numberNode(1), true),
                // strings
                Arguments.of("a", "a", true),
                Arguments.of("a", "b", false),
                Arguments.of("x", NODES.stringNode("x"), true),
                // booleans
                Arguments.of(true, true, true),
                Arguments.of(true, false, false),
                // the two unit values equal themselves but not each other
                Arguments.of(Null.INSTANCE, Null.INSTANCE, true),
                Arguments.of(Nothing.INSTANCE, Nothing.INSTANCE, true),
                Arguments.of(Nothing.INSTANCE, Null.INSTANCE, false),
                Arguments.of(NODES.nullNode(), Null.INSTANCE, true),
                Arguments.of(MissingNode.getInstance(), Nothing.INSTANCE, true),
                // cross-category comparisons are unequal (never throw)
                Arguments.of("1", 1L, false),
                Arguments.of(Null.INSTANCE, 1L, false),
                Arguments.of(Nothing.INSTANCE, "a", false));
    }

    @ParameterizedTest
    @MethodSource("equality")
    void equality(Object left, Object right, boolean expectedEqual) {
        // When / Then
        assertThat(RfcComparisons.compare(Ast.BinOp.EQ, left, right)).isEqualTo(expectedEqual);
        assertThat(RfcComparisons.compare(Ast.BinOp.NE, left, right)).isEqualTo(!expectedEqual);
    }

    static Stream<Arguments> ordering() {
        return Stream.of(
                // numbers
                Arguments.of(Ast.BinOp.LT, 1L, 2L, true),
                Arguments.of(Ast.BinOp.LT, 2L, 1L, false),
                Arguments.of(Ast.BinOp.LE, 1L, 1L, true),
                Arguments.of(Ast.BinOp.GT, 2L, 1L, true),
                Arguments.of(Ast.BinOp.GE, 1L, 1L, true),
                Arguments.of(Ast.BinOp.LT, 8L, NODES.numberNode(10), true),
                // strings
                Arguments.of(Ast.BinOp.LT, "a", "b", true),
                Arguments.of(Ast.BinOp.GT, "a", "b", false),
                // ordering is undefined (false) outside number-vs-number / string-vs-string
                Arguments.of(Ast.BinOp.LT, 1L, "b", false),
                Arguments.of(Ast.BinOp.LT, "b", 1L, false),
                Arguments.of(Ast.BinOp.LT, Nothing.INSTANCE, 1L, false),
                Arguments.of(Ast.BinOp.GT, Nothing.INSTANCE, 1L, false),
                Arguments.of(Ast.BinOp.LT, Null.INSTANCE, Null.INSTANCE, false),
                Arguments.of(Ast.BinOp.LT, true, false, false));
    }

    @ParameterizedTest
    @MethodSource("ordering")
    void ordering(Ast.BinOp op, Object left, Object right, boolean expected) {
        // When / Then
        assertThat(RfcComparisons.compare(op, left, right)).isEqualTo(expected);
    }

    @Test
    void orderedComparisonsCanBothBeFalseForIncomparableOperands() {
        // Given
        Object number = 1L;
        Object string = "a";

        // When / Then: x < y and y < x are both false when the operands are not comparable
        assertThat(RfcComparisons.compare(Ast.BinOp.LT, number, string)).isFalse();
        assertThat(RfcComparisons.compare(Ast.BinOp.LT, string, number)).isFalse();
    }
}
