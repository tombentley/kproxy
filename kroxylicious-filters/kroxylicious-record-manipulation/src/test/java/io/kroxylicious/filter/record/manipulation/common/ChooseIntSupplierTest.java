/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseIntSupplierTest {

    @Test
    void singleElementSetAlwaysReturnsThatElement() {
        // Given
        ChooseIntSupplier supplier = new ChooseIntSupplier(new Random(), Set.of(7));

        // When
        int value = supplier.getAsInt();

        // Then
        assertThat(value).isEqualTo(7);
    }

    @Test
    void everyDrawIsAMemberOfTheSuppliedSet() {
        // Given
        Set<Integer> from = Set.of(1, 2, 3);
        ChooseIntSupplier supplier = new ChooseIntSupplier(new Random(0), from);

        // When
        int[] drawn = IntStream.range(0, 200).map(i -> supplier.getAsInt()).toArray();

        // Then
        assertThat(IntStream.of(drawn).allMatch(from::contains)).isTrue();
    }

}
