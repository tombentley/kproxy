/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FunctionalTest {

    @Test
    void asFunctionIgnoresItsInputAndDelegatesToTheSupplier() {
        // Given
        Supplier<String> supplier = () -> "value";
        Function<Object, String> function = Functional.asFunction(supplier);

        // When
        String resultForOneInput = function.apply("input-a");
        String resultForAnotherInput = function.apply(null);

        // Then
        assertThat(resultForOneInput).isEqualTo("value");
        assertThat(resultForAnotherInput).isEqualTo("value");
    }

    @Test
    void asSupplierWithoutResultRunsTheRunnableAndReturnsNull() {
        // Given
        AtomicInteger invocations = new AtomicInteger();
        Supplier<String> supplier = Functional.asSupplier(invocations::incrementAndGet);

        // When
        String result = supplier.get();

        // Then
        assertThat(invocations).hasValue(1);
        assertThat(result).isNull();
    }

    @Test
    void asSupplierWithResultRunsTheRunnableAndReturnsTheGivenResult() {
        // Given
        AtomicInteger invocations = new AtomicInteger();
        Supplier<String> supplier = Functional.asSupplier(invocations::incrementAndGet, "result");

        // When
        String result = supplier.get();

        // Then
        assertThat(invocations).hasValue(1);
        assertThat(result).isEqualTo("result");
    }

    @Test
    void toFnRunsTheRunnableAndReturnsTheGivenResultRegardlessOfInput() {
        // Given
        AtomicInteger invocations = new AtomicInteger();
        Function<Object, String> function = Functional.toFn(invocations::incrementAndGet, "result");

        // When
        String resultForOneInput = function.apply("input-a");
        String resultForAnotherInput = function.apply("input-b");

        // Then
        assertThat(invocations).hasValue(2);
        assertThat(resultForOneInput).isEqualTo("result");
        assertThat(resultForAnotherInput).isEqualTo("result");
    }

}
