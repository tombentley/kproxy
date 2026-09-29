/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.ArrayList;
import java.util.List;

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

    // Filter selectors ('?<expr>') are parsed and type-checked (see AstBuilderTest / CheckTest / TypeCheckingVisitorTest)
    // but their runtime evaluation is not yet implemented (PathEvaluator throws UnsupportedOperationException), so
    // there are no runtime filter tests here yet.

}
