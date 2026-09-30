/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.neo.xform.path;

import java.util.List;
import java.util.Random;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.expr.ExprLexer;
import io.kroxylicious.filter.record.manipulation.expr.ExprParser;
import io.kroxylicious.filter.record.manipulation.neo.op.OpContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises general (multi-node) query evaluation — descendants, wildcards, indices, slices, unions of
 * selectors, and filter selectors embedded in a path — against the RFC 9535 example documents. This is the
 * navigation coverage that previously lived in {@code PathEvaluatorTest}, re-expressed against the AST
 * evaluator that replaces {@code PathEvaluator}.
 */
class AstQueryTest {

    private static final YAMLMapper YAML = new YAMLMapper();
    private static final OpContext OP_CONTEXT = new OpContext(new Random(0), new byte[0]);
    private static final AstEvaluator<JsonNode> EVALUATOR = new AstEvaluator<>(new Jackson3Tree());

    // RFC 9535 §1.5 example document.
    private static final JsonNode BOOKSTORE = YAML.readTree("""
            store:
              book:
                - {category: reference, author: "Nigel Rees", title: "Sayings of the Century", price: 8.95}
                - {category: fiction, author: "Evelyn Waugh", title: "Sword of Honour", price: 12.99}
                - {category: fiction, author: "Herman Melville", title: "Moby Dick", isbn: "0-553-21311-3", price: 8.99}
                - {category: fiction, author: "J. R. R. Tolkien", title: "The Lord of the Rings", isbn: "0-395-19395-8", price: 22.99}
              bicycle: {color: red, price: 399}
            """);

    // RFC 9535 §2.3.4.3 example document.
    private static final JsonNode LETTERS = YAML.readTree("[a, b, c, d, e, f, g]");

    private static Ast.Path<?> query(String expression) {
        var lexer = new ExprLexer(CharStreams.fromString(expression));
        var parser = new ExprParser(new CommonTokenStream(lexer));
        Ast ast = parser.expr().accept(new AstBuilder());
        return (Ast.Path<?>) ((Ast.Expr) ast).expr();
    }

    private static List<String> texts(JsonNode document, String expression) {
        return EVALUATOR.compileQuery(query(expression)).evaluate(document, OP_CONTEXT).stream()
                .map(JsonNode::asString)
                .toList();
    }

    private static List<String> authors(JsonNode document, String expression) {
        return EVALUATOR.compileQuery(query(expression)).evaluate(document, OP_CONTEXT).stream()
                .map(node -> node.get("author").asString())
                .toList();
    }

    @Test
    void descendantMatchesNameAtAnyDepth() {
        assertThat(texts(BOOKSTORE, "$..author"))
                .containsExactly("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien");
    }

    @Test
    void childSegmentsNavigateNamedAndWildcard() {
        assertThat(texts(BOOKSTORE, "$.store.book[*].author"))
                .containsExactly("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien");
    }

    @Test
    void wildcardSelectsAllChildren() {
        // the book array and the bicycle object
        assertThat(EVALUATOR.compileQuery(query("$.store.*")).evaluate(BOOKSTORE, OP_CONTEXT)).hasSize(2);
    }

    @Test
    void indexSelectorPicksArrayElement() {
        assertThat(texts(BOOKSTORE, "$..book[2].title")).containsExactly("Moby Dick");
    }

    @Test
    void indexSelectorCountsNegativeIndicesFromEnd() {
        assertThat(texts(BOOKSTORE, "$..book[-1].title")).containsExactly("The Lord of the Rings");
    }

    @Test
    void segmentAppliesUnionOfSelectors() {
        assertThat(texts(BOOKSTORE, "$..book[0,1].author")).containsExactly("Nigel Rees", "Evelyn Waugh");
    }

    @Test
    void nonMatchingChildPathYieldsNoResults() {
        assertThat(EVALUATOR.compileQuery(query("$.nonexistent.author")).evaluate(BOOKSTORE, OP_CONTEXT)).isEmpty();
    }

    @Test
    void filterSelectorSelectsMatchingElements() {
        // books priced under 10: "Sayings of the Century" (8.95) and "Moby Dick" (8.99)
        assertThat(authors(BOOKSTORE, "$.store.book[?@.price < 10]"))
                .containsExactly("Nigel Rees", "Herman Melville");
    }

    @Test
    void sliceSelectsHalfOpenRange() {
        assertThat(texts(LETTERS, "$[1:3]")).containsExactly("b", "c");
    }

    @Test
    void sliceWithoutEndRunsToArrayEnd() {
        assertThat(texts(LETTERS, "$[5:]")).containsExactly("f", "g");
    }

    @Test
    void sliceAppliesStep() {
        assertThat(texts(LETTERS, "$[1:5:2]")).containsExactly("b", "d");
    }

    @Test
    void sliceNormalisesNegativeStart() {
        assertThat(texts(LETTERS, "$[-3:]")).containsExactly("e", "f", "g");
    }

    @Test
    void negativeStepSliceSelectsReversedRange() {
        // RFC 9535 §2.3.4.2: a negative step yields the range in reverse order.
        assertThat(texts(LETTERS, "$[5:1:-2]")).containsExactly("f", "d");
    }

    @Test
    void descendantSliceSelectsFromArraysAtAnyDepth() {
        assertThat(authors(BOOKSTORE, "$..[0:2]")).containsExactly("Nigel Rees", "Evelyn Waugh");
    }
}
