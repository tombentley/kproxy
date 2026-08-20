/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseLongSupplierTest {

    @Test
    void singleElementSetAlwaysReturnsThatElement() {
        // Given
        ChooseLongSupplier supplier = new ChooseLongSupplier(new Random(), Set.of(7L));

        // When
        long value = supplier.getAsLong();

        // Then
        assertThat(value).isEqualTo(7L);
    }

    @Test
    void everyDrawIsAMemberOfTheSuppliedSet() {
        // Given
        Set<Long> from = Set.of(1L, 2L, 3L);
        ChooseLongSupplier supplier = new ChooseLongSupplier(new Random(0), from);

        // When
        long[] drawn = LongStream.range(0, 200).map(i -> supplier.getAsLong()).toArray();

        // Then
        assertThat(LongStream.of(drawn).allMatch(from::contains)).isTrue();
    }

}
