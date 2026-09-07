/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.lang.reflect.Type;

import org.junit.jupiter.api.Test;

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
    void testWithLambda() {
        BaseTypedOp<?, ?> op = BaseTypedOp.of(String.class, Integer.class, (String str, OpContext context) -> null);
        assertThat(op.inputType()).isEqualTo(String.class);
        assertThat(op.outputType()).isEqualTo(Integer.class);
        assertThat(op.typeParameters()).isEmpty();
    }

    @Test
    void testWithAnonClass() {
        StaticTypedOp<?, ?> op = new StaticTypedOp<String, Integer>() {
            @Override
            public Type outputType(Type inputType) {
                return null;
            }

            @Override
            public Integer apply(String str, OpContext context) {
                return 0;
            }
        };
        assertThat(op.inputType()).isEqualTo(String.class);
        assertThat(op.outputType()).isEqualTo(Integer.class);
        assertThat(op.typeParameters()).isEmpty();
    }

}