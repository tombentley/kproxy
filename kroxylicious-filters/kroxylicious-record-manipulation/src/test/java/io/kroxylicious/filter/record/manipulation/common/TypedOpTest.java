/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.leangen.geantyref.TypeFactory;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;

class TypedOpTest {

    @Test
    void identity() {
        BaseTypedOp<?, ?> op = IdentityOp.identity();
        assertThat(op.typeParameters()).isNotEmpty();
    }

    @Test
    void testWithClassLiterals() {
        BaseTypedOp<?, ?> op = BaseTypedOp.of(String.class, Integer.class, (String str, OpContext context) -> null);
        assertThat(op.inputType()).isEqualTo(String.class);
        assertThat(op.outputType()).isEqualTo(Integer.class);
        assertThat(op.typeParameters()).isEmpty();
    }

    @Test
    void testWithRuntimeComputedType() {
        // Simulates the case (e.g. ListFirst) where T/R aren't statically known at the call site, so
        // Class literals aren't available - only a Type value computed from something else at runtime.
        Type listOfStrings = TypeFactory.parameterizedClass(List.class, String.class);
        BaseTypedOp<?, ?> op = BaseTypedOp.of(listOfStrings, String.class, (List<?> list, OpContext context) -> null);
        assertThat(op.inputType()).isEqualTo(listOfStrings);
        assertThat(op.outputType()).isEqualTo(String.class);
    }

    @Test
    void testWithAnonClassForParameterizedType() {
        // StaticTypedOp is only needed when T/R is a genuinely parameterized type that can't be
        // expressed as a bare Class literal - mirrors JacksonFunction.asMaybe()'s real use of it.
        StaticTypedOp<List<String>, String> op = new StaticTypedOp<List<String>, String>() {
            @Override
            public String apply(List<String> value, OpContext context) {
                return String.join(",", value);
            }
        };
        assertThat(op.inputType()).isInstanceOfSatisfying(ParameterizedType.class, pt -> {
            assertThat(pt.getRawType()).isEqualTo(List.class);
            assertThat(pt.getActualTypeArguments()).containsExactly(String.class);
        });
        assertThat(op.outputType()).isEqualTo(String.class);
        assertThat(op.typeParameters()).isEmpty();
    }

}