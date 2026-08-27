/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextPipelineTest {

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    @Test
    void emptyPipelineComposes() {
        // Given/When/Then
        assertThatCode(() -> new ContextPipeline<>(List.of())).doesNotThrowAnyException();
    }

    @Test
    void singleFunctionPipelineComposes() {
        // Given/When/Then
        assertThatCode(() -> new ContextPipeline<>(List.of(TypedOp.of(String.class, Integer.class, (s, ctx) -> s.length()))))
                .doesNotThrowAnyException();
    }

    @Test
    void compatibleReturnAndParameterTypesCompose() {
        // Given/When/Then
        assertThatCode(() -> new ContextPipeline<>(List.of(
                TypedOp.of(String.class, Integer.class, (s, ctx) -> s.length()),
                TypedOp.of(Integer.class, String.class, (i, ctx) -> i.toString()))))
                .doesNotThrowAnyException();
    }

    @Test
    void incompatibleReturnAndParameterTypesDoNotCompose() {
        // Given/When/Then
        assertThatThrownBy(() -> new ContextPipeline<>(List.of(
                TypedOp.of(String.class, Integer.class, (s, ctx) -> s.length()),
                TypedOp.of(Double.class, String.class, (d, ctx) -> d.toString()))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("do not compose");
    }

    @Test
    void appliesFunctionsInOrderThreadingTheSameContext() {
        // Given
        ContextPipeline<String, String> pipeline = new ContextPipeline<>(List.of(
                TypedOp.of(String.class, Integer.class, (s, ctx) -> s.length()),
                TypedOp.of(Integer.class, String.class, (i, ctx) -> i.toString())));

        // When
        String result = pipeline.apply("hello", CONTEXT);

        // Then
        assertThat(result).isEqualTo("5");
    }

    @Test
    void typePreservingChainSatisfiesTheRequirement() {
        // Given/When/Then
        assertThatCode(() -> new ContextPipeline<>(List.of(
                TypedOp.of(String.class, (value, ctx) -> value + "!"),
                TypedOp.of(String.class, (value, ctx) -> value + "!")), Set.of(Requirement.TYPE_PRESERVING)))
                .doesNotThrowAnyException();
    }

    @Test
    void nonTypePreservingChainFailsOnlyWhenRequirementIsRequested() {
        // Given
        List<TypedOp<?, ?>> ops = List.of(
                TypedOp.of(String.class, Integer.class, (s, ctx) -> s.length()),
                TypedOp.of(Integer.class, String.class, (i, ctx) -> i.toString()),
                TypedOp.of(String.class, Integer.class, (s, ctx) -> s.length()));

        // When/Then
        assertThatCode(() -> new ContextPipeline<>(ops)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new ContextPipeline<>(ops, Set.of(Requirement.TYPE_PRESERVING)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not type-preserving");
    }

}
