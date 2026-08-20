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

    @Test
    void singleElementSetAlwaysReturnsThatElement() {
        // Given
        ChooseDoubleSupplier supplier = new ChooseDoubleSupplier(new Random(), Set.of(7.5));

        // When
        double value = supplier.getAsDouble();

        // Then
        assertThat(value).isEqualTo(7.5);
    }

    @Test
    void everyDrawIsAMemberOfTheSuppliedSet() {
        // Given
        Set<Double> from = Set.of(1.5, 2.5, 3.5);
        ChooseDoubleSupplier supplier = new ChooseDoubleSupplier(new Random(0), from);

        // When
        double[] drawn = IntStream.range(0, 200).mapToDouble(i -> supplier.getAsDouble()).toArray();

        // Then
        assertThat(DoubleStream.of(drawn).allMatch(from::contains)).isTrue();
    }

}
