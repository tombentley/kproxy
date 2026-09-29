/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.expr.ExprLexer;
import io.kroxylicious.filter.record.manipulation.expr.ExprParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a filter selector ({@code [?<expr>]}) is built into a real {@link Ast} sub-tree, including the
 * relative ({@code @}) and absolute ({@code $}) queries that appear inside it.
 */
class AstBuilderTest {

    private static Ast parse(String expression) {
        var lexer = new ExprLexer(CharStreams.fromString(expression));
        var parser = new ExprParser(new CommonTokenStream(lexer));
        return parser.expr().accept(new AstBuilder());
    }

    /** The expression sub-tree of the first filter selector found while walking the (single) path of {@code expression}. */
    private static Ast filterExpression(String expression) {
        Ast.Path<?> path = (Ast.Path<?>) ((Ast.Expr) parse(expression)).expr();
        for (var segment : path.segments()) {
            if (segment instanceof Ast.Segment.Child<?> child) {
                for (var selector : child.selectors()) {
                    if (selector instanceof Ast.Selector.FilterFilter<?> filter) {
                        return filter.expression();
                    }
                }
            }
        }
        throw new AssertionError("no filter selector found in " + expression);
    }

    /** Asserts {@code path} is a single-segment name path and returns that name. */
    private static String singleName(Ast node) {
        assertThat(node).isInstanceOf(Ast.Path.class);
        Ast.Path<?> path = (Ast.Path<?>) node;
        assertThat(path.segments()).hasSize(1);
        Ast.Segment<?> segment = path.segments().get(0);
        assertThat(segment).isInstanceOf(Ast.Segment.Child.class);
        var selectors = ((Ast.Segment.Child<?>) segment).selectors();
        assertThat(selectors).hasSize(1);
        assertThat(selectors.get(0)).isInstanceOf(Ast.Selector.Name.class);
        return ((Ast.Selector.Name<?>) selectors.get(0)).name();
    }

    @Test
    void comparisonFilterCarriesBinaryOpSubtree() {
        // When
        Ast expression = filterExpression("$.a[?@.price < 10]");

        // Then
        assertThat(expression).isInstanceOf(Ast.BinaryOp.class);
        Ast.BinaryOp binaryOp = (Ast.BinaryOp) expression;
        assertThat(binaryOp.op()).isEqualTo(Ast.BinOp.LT);
        assertThat(((Ast.Path<?>) binaryOp.left()).identifier()).isEqualTo(Identifier.CURRENT);
        assertThat(singleName(binaryOp.left())).isEqualTo("price");
        assertThat(binaryOp.right()).isInstanceOf(Ast.Int.class);
        assertThat(((Ast.Int) binaryOp.right()).value()).isEqualTo(10L);
    }

    @Test
    void existenceFilterCarriesRelativePathSubtree() {
        // When
        Ast expression = filterExpression("$..book[?@.isbn]");

        // Then
        assertThat(expression).isInstanceOf(Ast.Path.class);
        assertThat(((Ast.Path<?>) expression).identifier()).isEqualTo(Identifier.CURRENT);
        assertThat(singleName(expression)).isEqualTo("isbn");
    }

    @Test
    void comparisonFilterCanReferenceBothCurrentAndRoot() {
        // When
        Ast expression = filterExpression("$.a[?@.b == $.x]");

        // Then
        assertThat(expression).isInstanceOf(Ast.BinaryOp.class);
        Ast.BinaryOp binaryOp = (Ast.BinaryOp) expression;
        assertThat(binaryOp.op()).isEqualTo(Ast.BinOp.EQ);
        assertThat(((Ast.Path<?>) binaryOp.left()).identifier()).isEqualTo(Identifier.CURRENT);
        assertThat(((Ast.Path<?>) binaryOp.right()).identifier()).isEqualTo(Identifier.ROOT);
    }
}
