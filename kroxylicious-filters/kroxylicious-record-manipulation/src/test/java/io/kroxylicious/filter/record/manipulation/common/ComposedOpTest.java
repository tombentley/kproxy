/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.TypeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComposedOpTest {

    OpContext opContext = new OpContext(new Random(), new byte[0]);

    @Test
    void appliesFirstThenThen() {
        // Given
        BaseTypedOp<String, Integer> parseInt = BaseTypedOp.of(String.class, Integer.class,
                (String s, OpContext ctx) -> Integer.parseInt(s) * 2);
        BaseTypedOp<Integer, String> toString = BaseTypedOp.of(Integer.class, String.class,
                (Integer i, OpContext ctx) -> "value=" + i);
        var composed = new ComposedOp<>(parseInt, toString);

        // When
        String result = composed.apply("21", opContext);

        // Then
        assertThat(result).isEqualTo("value=42");
    }

    @Test
    void exposesInputAndOutputTypesOfItsStages() {
        // Given
        BaseTypedOp<String, Integer> first = BaseTypedOp.of(String.class, Integer.class, (s, ctx) -> 0);
        BaseTypedOp<Integer, Boolean> then = BaseTypedOp.of(Integer.class, Boolean.class, (i, ctx) -> true);

        // When
        var composed = new ComposedOp<>(first, then);

        // Then
        assertThat(composed.inputType()).isEqualTo(String.class);
        assertThat(composed.outputType()).isEqualTo(Boolean.class);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void rejectsStagesWhoseTypesDoNotCompose() {
        // Given: first's declared output type (String) doesn't match then's declared input type
        // (Integer) - raw types are needed to get past compile-time generic checking, mirroring how
        // OpConfigs.compose builds a chain from operations whose types are only known at runtime.
        BaseTypedOp first = BaseTypedOp.of(String.class, String.class, (String s, OpContext ctx) -> s);
        BaseTypedOp then = BaseTypedOp.of(Integer.class, String.class, (Integer i, OpContext ctx) -> i.toString());

        // When/Then
        assertThatThrownBy(() -> new ComposedOp(first, then))
                .isInstanceOf(TypeException.class);
    }

}
