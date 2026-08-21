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

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void singleElementSetAlwaysReturnsThatElement() {
        // Given
        ChooseIntSupplier supplier = new ChooseIntSupplier(Set.of(7));

        // When
        int value = supplier.applyAsInt(contextWithSeed(0));

        // Then
        assertThat(value).isEqualTo(7);
    }

    @Test
    void everyDrawIsAMemberOfTheSuppliedSet() {
        // Given
        Set<Integer> from = Set.of(1, 2, 3);
        ChooseIntSupplier supplier = new ChooseIntSupplier(from);
        Context context = contextWithSeed(0);

        // When
        int[] drawn = IntStream.range(0, 200).map(i -> supplier.applyAsInt(context)).toArray();

        // Then
        assertThat(IntStream.of(drawn).allMatch(from::contains)).isTrue();
    }

}
