/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Note: the functions under test are named classes, not lambdas. Pipeline inspects the
 * reified generic type arguments of each function's class, which lambdas erase.
 */
class PipelineTest {

    @Test
    void emptyPipelineComposes() {
        // Given/When/Then
        assertThatCode(() -> new Pipeline(List.of())).doesNotThrowAnyException();
    }

    @Test
    void singleFunctionPipelineComposes() {
        // Given/When/Then
        assertThatCode(() -> new Pipeline(List.of(new StringLength()))).doesNotThrowAnyException();
    }

    @Test
    void compatibleReturnAndParameterTypesCompose() {
        // Given/When/Then
        assertThatCode(() -> new Pipeline(List.of(new StringLength(), new IntegerToString())))
                .doesNotThrowAnyException();
    }

    @Test
    void incompatibleReturnAndParameterTypesDoNotCompose() {
        // Given/When/Then
        assertThatThrownBy(() -> new Pipeline(List.of(new StringLength(), new DoubleToString())))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("do not compose");
    }

    private static class StringLength implements Function<String, Integer> {
        @Override
        public Integer apply(String s) {
            return s.length();
        }
    }

    private static class IntegerToString implements Function<Integer, String> {
        @Override
        public String apply(Integer i) {
            return i.toString();
        }
    }

    private static class DoubleToString implements Function<Double, String> {
        @Override
        public String apply(Double d) {
            return d.toString();
        }
    }

}
