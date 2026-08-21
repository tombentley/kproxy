/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Note: the functions under test are named classes, not lambdas. ContextPipeline inspects the reified
 * generic type arguments of each function's class, which lambdas erase.
 */
class ContextPipelineTest {

    private static final Context CONTEXT = new Context(new Random(), new byte[0]);

    @Test
    void emptyPipelineComposes() {
        // Given/When/Then
        assertThatCode(() -> new ContextPipeline(List.of())).doesNotThrowAnyException();
    }

    @Test
    void singleFunctionPipelineComposes() {
        // Given/When/Then
        assertThatCode(() -> new ContextPipeline(List.of(new StringLength()))).doesNotThrowAnyException();
    }

    @Test
    void compatibleReturnAndParameterTypesCompose() {
        // Given/When/Then
        assertThatCode(() -> new ContextPipeline(List.of(new StringLength(), new IntegerToString())))
                .doesNotThrowAnyException();
    }

    @Test
    void incompatibleReturnAndParameterTypesDoNotCompose() {
        // Given/When/Then
        assertThatThrownBy(() -> new ContextPipeline(List.of(new StringLength(), new DoubleToString())))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("do not compose");
    }

    @Test
    void appliesFunctionsInOrderThreadingTheSameContext() {
        // Given
        ContextPipeline pipeline = new ContextPipeline(List.of(new StringLength(), new IntegerToString()));

        // When
        String result = pipeline.apply("hello", CONTEXT);

        // Then
        assertThat(result).isEqualTo("5");
    }

    @Test
    void typePreservingChainSatisfiesTheRequirement() {
        // Given/When/Then
        assertThatCode(() -> new ContextPipeline(List.of(new AppendExclamation(), new AppendExclamation()), Set.of(Requirement.TYPE_PRESERVING)))
                .doesNotThrowAnyException();
    }

    @Test
    void nonTypePreservingChainFailsOnlyWhenRequirementIsRequested() {
        // Given
        List<BiFunction<?, Context, ?>> ops = List.of(new StringLength(), new IntegerToString(), new StringLength());

        // When/Then
        assertThatCode(() -> new ContextPipeline(ops)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new ContextPipeline(ops, Set.of(Requirement.TYPE_PRESERVING)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not type-preserving");
    }

    private static class StringLength implements BiFunction<String, Context, Integer> {
        @Override
        public Integer apply(String s, Context context) {
            return s.length();
        }
    }

    private static class IntegerToString implements BiFunction<Integer, Context, String> {
        @Override
        public String apply(Integer i, Context context) {
            return i.toString();
        }
    }

    private static class DoubleToString implements BiFunction<Double, Context, String> {
        @Override
        public String apply(Double d, Context context) {
            return d.toString();
        }
    }

    private static class AppendExclamation implements StringOp {
        @Override
        public String apply(String value, Context context) {
            return value + "!";
        }
    }

}
