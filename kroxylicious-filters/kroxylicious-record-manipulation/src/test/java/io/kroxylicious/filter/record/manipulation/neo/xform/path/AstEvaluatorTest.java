/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.neo.xform.path;

import java.util.Random;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.expr.ExprLexer;
import io.kroxylicious.filter.record.manipulation.expr.ExprParser;
import io.kroxylicious.filter.record.manipulation.neo.op.OpContext;
import io.kroxylicious.filter.record.manipulation.neo.type.Nothing;
import io.kroxylicious.filter.record.manipulation.neo.type.Null;

import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evaluates filter predicates ({@code [?...]} bodies) end-to-end: parse &rarr; extract the filter body &rarr;
 * compile &rarr; test candidate nodes ({@code @}) against a root ({@code $}). Exercises comparisons, logical
 * combinators, existence, and the {@link Null Null} vs
 * {@link Nothing Nothing} distinction.
 */
class AstEvaluatorTest {

    private static final YAMLMapper YAML = new YAMLMapper();
    private static final OpContext OP_CONTEXT = new OpContext(new Random(0), new byte[0]);
    private static final AstEvaluator<JsonNode> EVALUATOR = new AstEvaluator<>(new Jackson3Tree());

    private static JsonNode json(String yaml) {
        return YAML.readTree(yaml);
    }

    /** The body of the first filter selector in {@code expression}, compiled to a predicate. */
    private static AstEvaluator.CompiledPredicate<JsonNode> predicate(String expression) {
        var lexer = new ExprLexer(CharStreams.fromString(expression));
        var parser = new ExprParser(new CommonTokenStream(lexer));
        Ast ast = parser.expr().accept(new AstBuilder());
        Ast.Path<?> path = (Ast.Path<?>) ((Ast.Expr) ast).expr();
        for (var segment : path.segments()) {
            if (segment instanceof Ast.Segment.Child<?> child) {
                for (var selector : child.selectors()) {
                    if (selector instanceof Ast.Selector.Filter<?> filter) {
                        return EVALUATOR.compilePredicate(filter.expression());
                    }
                }
            }
        }
        throw new AssertionError("no filter selector in " + expression);
    }

    private static boolean test(AstEvaluator.CompiledPredicate<JsonNode> predicate, JsonNode candidate) {
        return predicate.test(candidate, candidate, OP_CONTEXT);
    }

    @Test
    void comparisonSelectsElementsSatisfyingThePredicate() {
        // Given
        var predicate = predicate("$.a[?@.price < 10]");

        // When / Then
        assertThat(test(predicate, json("price: 8"))).isTrue();
        assertThat(test(predicate, json("price: 12"))).isFalse();
    }

    @Test
    void comparisonAgainstAbsentMemberIsFalse() {
        // Given
        var predicate = predicate("$.a[?@.price < 10]");

        // When / Then: @.price is Nothing, and ordering against Nothing is false (never throws)
        assertThat(test(predicate, json("{}"))).isFalse();
    }

    @Test
    void existenceTestDistinguishesPresentFromAbsent() {
        // Given
        var predicate = predicate("$.a[?@.isbn]");

        // When / Then
        assertThat(test(predicate, json("isbn: \"0-395-19395-8\""))).isTrue();
        assertThat(test(predicate, json("{}"))).isFalse();
    }

    @Test
    void existenceOfAnExplicitNullMemberIsTrue() {
        // Given
        var predicate = predicate("$.a[?@.isbn]");

        // When / Then: a member that is present but null still exists
        assertThat(test(predicate, json("isbn: null"))).isTrue();
    }

    @Test
    void equalityDistinguishesNullFromAbsent() {
        // Given
        var predicate = predicate("$.a[?@.b == null]");

        // When / Then: a present null equals null; an absent member (Nothing) does not
        assertThat(test(predicate, json("b: null"))).isTrue();
        assertThat(test(predicate, json("{}"))).isFalse();
    }

    @Test
    void conjunctionShortCircuitsAcrossMembers() {
        // Given
        var predicate = predicate("$.a[?@.category == \"fiction\" && @.price < 10]");

        // When / Then
        assertThat(test(predicate, json("{category: fiction, price: 8}"))).isTrue();
        assertThat(test(predicate, json("{category: fiction, price: 20}"))).isFalse();
        assertThat(test(predicate, json("{category: reference, price: 8}"))).isFalse();
    }

    @Test
    void predicateCanReferenceBothCurrentAndRoot() {
        // Given
        var predicate = predicate("$.a[?@.b == $.x]");
        JsonNode root = json("x: 5");

        // When / Then: @ is the candidate, $ is the root
        assertThat(predicate.test(root, json("b: 5"), OP_CONTEXT)).isTrue();
        assertThat(predicate.test(root, json("b: 6"), OP_CONTEXT)).isFalse();
    }

    @Test
    void disjunctionSelectsWhenEitherHolds() {
        // Given
        var predicate = predicate("$.a[?@.price < 10 || @.category == \"fiction\"]");

        // When / Then
        assertThat(test(predicate, json("{category: reference, price: 8}"))).isTrue();
        assertThat(test(predicate, json("{category: fiction, price: 20}"))).isTrue();
        assertThat(test(predicate, json("{category: reference, price: 20}"))).isFalse();
    }
}
