/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class PathEvaluatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // RFC 9535 §1.5 example document.
    private static final JsonNode BOOKSTORE = bookstore();

    // RFC 9535 §2.3.4.3 example document.
    private static final JsonNode LETTERS = read("[\"a\", \"b\", \"c\", \"d\", \"e\", \"f\", \"g\"]");

    // RFC 9535 §2.3.5.3 example document (filter selector).
    private static final JsonNode FILTER_DOC = read("""
            { "a": [3, 5, 1, 2, 4, 6, {"b": "j"}, {"b": "k"}, {"b": {}}, {"b": "kilo"}],
              "o": {"p": 1, "q": 2, "r": 3, "s": 5, "t": {"u": 6}},
              "e": "f" }
            """);

    private static JsonNode read(String json) {
        try {
            return MAPPER.readTree(json);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode bookstore() {
        try {
            return MAPPER.readTree("""
                    { "store": {
                        "book": [
                          { "category": "reference", "author": "Nigel Rees", "title": "Sayings of the Century", "price": 8.95 },
                          { "category": "fiction", "author": "Evelyn Waugh", "title": "Sword of Honour", "price": 12.99 },
                          { "category": "fiction", "author": "Herman Melville", "title": "Moby Dick", "isbn": "0-553-21311-3", "price": 8.99 },
                          { "category": "fiction", "author": "J. R. R. Tolkien", "title": "The Lord of the Rings", "isbn": "0-395-19395-8", "price": 22.99 }
                        ],
                        "bicycle": { "color": "red", "price": 399 }
                      }
                    }
                    """);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<JsonNode> eval(Ast.Segment<JsonNode>... segments) {
        return evalOn(BOOKSTORE, segments);
    }

    private static List<JsonNode> evalOn(JsonNode document, Ast.Segment<JsonNode>... segments) {
        var results = new ArrayList<JsonNode>();
        new PathEvaluator<>(new Jackson2Tree()).eval(document, List.of(new Ast.Path<>(Identifier.ROOT, List.of(segments), results::add)));
        return results;
    }

    @Test
    void descendantMatchesNameAtAnyDepth() {
        // When
        var authors = eval(new Ast.Segment.Descendant<>(new Ast.Selector.Name<>("author")));

        // Then
        assertThat(authors).map(JsonNode::asText)
                .containsExactly("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien");
    }

    @Test
    void childSegmentsNavigateNamedAndWildcard() {
        // When
        var authors = eval(
                new Ast.Segment.Child<>(new Ast.Selector.Name<>("store")),
                new Ast.Segment.Child<>(new Ast.Selector.Name<>("book")),
                new Ast.Segment.Child<>(new Ast.Selector.Children<>()),
                new Ast.Segment.Child<>(new Ast.Selector.Name<>("author")));

        // Then
        assertThat(authors).map(JsonNode::asText)
                .containsExactly("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien");
    }

    @Test
    void wildcardSelectsAllChildren() {
        // When
        var children = eval(new Ast.Segment.Child<>(new Ast.Selector.Name<>("store")), new Ast.Segment.Child<>(new Ast.Selector.Children<>()));

        // Then
        assertThat(children).hasSize(2); // the book array and the bicycle object
    }

    @Test
    void indexSelectorPicksArrayElement() {
        // When
        var titles = eval(
                new Ast.Segment.Descendant<>(new Ast.Selector.Name<>("book")),
                new Ast.Segment.Child<>(new Ast.Selector.Index<>(2)),
                new Ast.Segment.Child<>(new Ast.Selector.Name<>("title")));

        // Then
        assertThat(titles).map(JsonNode::asText).containsExactly("Moby Dick");
    }

    @Test
    void indexSelectorCountsNegativeIndicesFromEnd() {
        // When
        var titles = eval(
                new Ast.Segment.Descendant<>(new Ast.Selector.Name<>("book")),
                new Ast.Segment.Child<>(new Ast.Selector.Index<>(-1)),
                new Ast.Segment.Child<>(new Ast.Selector.Name<>("title")));

        // Then
        assertThat(titles).map(JsonNode::asText).containsExactly("The Lord of the Rings");
    }

    @Test
    void segmentAppliesUnionOfSelectors() {
        // When
        var authors = eval(
                new Ast.Segment.Descendant<>(new Ast.Selector.Name<>("book")),
                new Ast.Segment.Child<>(List.of(new Ast.Selector.Index<>(0), new Ast.Selector.Index<>(1))),
                new Ast.Segment.Child<>(new Ast.Selector.Name<>("author")));

        // Then
        assertThat(authors).map(JsonNode::asText).containsExactly("Nigel Rees", "Evelyn Waugh");
    }

    @Test
    void evaluatesMultiplePathsInOnePass() {
        // Given
        var authors = new ArrayList<JsonNode>();
        var prices = new ArrayList<JsonNode>();

        // When
        new PathEvaluator<>(new Jackson2Tree()).eval(BOOKSTORE, List.of(
                new Ast.Path<>(Identifier.ROOT, List.of(new Ast.Segment.Descendant<>(new Ast.Selector.Name<>("author"))), authors::add),
                new Ast.Path<>(Identifier.ROOT, List.of(new Ast.Segment.Descendant<>(new Ast.Selector.Name<>("price"))), prices::add)));

        // Then
        assertThat(authors).map(JsonNode::asText)
                .containsExactly("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien");
        assertThat(prices).map(JsonNode::asDouble)
                .containsExactly(8.95, 12.99, 8.99, 22.99, 399.0);
    }

    @Test
    void nonMatchingChildPathYieldsNoResults() {
        // When
        var none = eval(new Ast.Segment.Child<>(new Ast.Selector.Name<>("nonexistent")), new Ast.Segment.Child<>(new Ast.Selector.Name<>("author")));

        // Then
        assertThat(none).isEmpty();
    }

    @Test
    void sliceSelectsHalfOpenRange() {
        // When
        var letters = evalOn(LETTERS, new Ast.Segment.Child<>(new Ast.Selector.Slice<>(1, 3)));

        // Then
        assertThat(letters).map(JsonNode::asText).containsExactly("b", "c");
    }

    @Test
    void sliceWithoutEndRunsToArrayEnd() {
        // When
        var letters = evalOn(LETTERS, new Ast.Segment.Child<>(new Ast.Selector.Slice<>(5, null)));

        // Then
        assertThat(letters).map(JsonNode::asText).containsExactly("f", "g");
    }

    @Test
    void sliceAppliesStep() {
        // When
        var letters = evalOn(LETTERS, new Ast.Segment.Child<>(new Ast.Selector.Slice<>(1, 5, 2)));

        // Then
        assertThat(letters).map(JsonNode::asText).containsExactly("b", "d");
    }

    @Test
    void sliceNormalisesNegativeStart() {
        // When
        var letters = evalOn(LETTERS, new Ast.Segment.Child<>(new Ast.Selector.Slice<>(-3, null)));

        // Then
        assertThat(letters).map(JsonNode::asText).containsExactly("e", "f", "g");
    }

    @Test
    void negativeStepSliceSelectsReversedRange() {
        // When
        var letters = evalOn(LETTERS, new Ast.Segment.Child<>(new Ast.Selector.Slice<>(5, 1, -2)));

        // Then
        // RFC 9535 yields ["f", "d"]; the single-pass traversal emits matches in document order.
        assertThat(letters).map(JsonNode::asText).containsExactly("d", "f");
    }

    @Test
    void descendantSliceSelectsFromArraysAtAnyDepth() {
        // When
        var firstTwoBooks = eval(new Ast.Segment.Descendant<>(new Ast.Selector.Slice<>(0, 2)));

        // Then
        assertThat(firstTwoBooks).map(book -> book.get("author").asText())
                .containsExactly("Nigel Rees", "Evelyn Waugh");
    }

    @Test
    void childFilterSelectsArrayElementsSatisfyingPredicate() {
        // When: $.store.book[?@.price < 10]
        var cheap = eval(
                new Ast.Segment.Child<>(new Ast.Selector.Name<>("store")),
                new Ast.Segment.Child<>(new Ast.Selector.Name<>("book")),
                new Ast.Segment.Child<>(new Ast.Selector.FilterFilter<>((JsonNode node, JsonNode root) -> node.path("price").asDouble() < 10)));

        // Then
        assertThat(cheap).map(book -> book.get("title").asText())
                .containsExactly("Sayings of the Century", "Moby Dick");
    }

    @Test
    void childFilterExistenceTestSelectsMembers() {
        // When: $..book[?@.isbn]
        var withIsbn = eval(
                new Ast.Segment.Descendant<>(new Ast.Selector.Name<>("book")),
                new Ast.Segment.Child<>(new Ast.Selector.FilterFilter<>((JsonNode node, JsonNode root) -> node.has("isbn"))));

        // Then
        assertThat(withIsbn).map(book -> book.get("title").asText())
                .containsExactly("Moby Dick", "The Lord of the Rings");
    }

    @Test
    void filterSelectsObjectMemberValues() {
        // When: $.store[?@.price] — of store's members, only the bicycle has a price
        var priced = eval(
                new Ast.Segment.Child<>(new Ast.Selector.Name<>("store")),
                new Ast.Segment.Child<>(new Ast.Selector.FilterFilter<>((JsonNode node, JsonNode root) -> node.has("price"))));

        // Then
        assertThat(priced).map(node -> node.get("color").asText()).containsExactly("red");
    }

    @Test
    void descendantFilterMatchesAtAnyDepth() {
        // When: $..[?@.price < 10]
        var cheap = eval(new Ast.Segment.Descendant<>(
                new Ast.Selector.FilterFilter<>((JsonNode node, JsonNode root) -> node.has("price") && node.get("price").asDouble() < 10)));

        // Then
        assertThat(cheap).map(book -> book.get("title").asText())
                .containsExactly("Sayings of the Century", "Moby Dick");
    }

    @Test
    void filterCanReferenceRoot() {
        // When: $.a[?@.b == $.x] — $.x is absent, so it matches elements whose 'b' is also absent (RFC 9535 §2.3.5.3)
        var matched = evalOn(FILTER_DOC,
                new Ast.Segment.Child<>(new Ast.Selector.Name<>("a")),
                new Ast.Segment.Child<>(new Ast.Selector.FilterFilter<>((JsonNode node, JsonNode root) -> Objects.equals(node.get("b"), root.get("x")))));

        // Then
        assertThat(matched).map(JsonNode::asInt).containsExactly(3, 5, 1, 2, 4, 6);
    }

    @Test
    void filterCanInitiateNestedTraversalFromRoot() {
        // When: $.a[?@ == $.o.p] where '$.o.p' is resolved by a nested traversal from the root
        var matched = evalOn(FILTER_DOC,
                new Ast.Segment.Child<>(new Ast.Selector.Name<>("a")),
                new Ast.Segment.Child<>(new Ast.Selector.FilterFilter<>((JsonNode node, JsonNode root) -> node.equals(queryOne(root,
                        new Ast.Segment.Child<>(new Ast.Selector.Name<>("o")),
                        new Ast.Segment.Child<>(new Ast.Selector.Name<>("p")))))));

        // Then
        assertThat(matched).map(JsonNode::asInt).containsExactly(1);
    }

    /** Runs a nested query against {@code node}, returning its single result (or {@code null} if there is none). */
    private static JsonNode queryOne(JsonNode node, Ast.Segment<JsonNode>... segments) {
        var results = new ArrayList<JsonNode>();
        new PathEvaluator<>(new Jackson2Tree()).eval(node, List.of(new Ast.Path<>(Identifier.ROOT, List.of(segments), results::add)));
        return results.isEmpty() ? null : results.getFirst();
    }

}
