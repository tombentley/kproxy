/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseDoubleSupplierTest {

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void singleElementSetAlwaysReturnsThatElement() {
        // Given
        ChooseDoubleSupplier supplier = new ChooseDoubleSupplier(Set.of(7.5));

        // When
        double value = supplier.applyAsDouble(contextWithSeed(0));

        // Then
        assertThat(value).isEqualTo(7.5);
    }

    @Test
    void everyDrawIsAMemberOfTheSuppliedSet() {
        // Given
        Set<Double> from = Set.of(1.5, 2.5, 3.5);
        ChooseDoubleSupplier supplier = new ChooseDoubleSupplier(from);
        Context context = contextWithSeed(0);

        // When
        double[] drawn = IntStream.range(0, 200).mapToDouble(i -> supplier.applyAsDouble(context)).toArray();

        // Then
        assertThat(DoubleStream.of(drawn).allMatch(from::contains)).isTrue();
    }

}
