/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseFloatSupplierTest {

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void singleElementSetAlwaysReturnsThatElement() {
        // Given
        ChooseFloatSupplier supplier = new ChooseFloatSupplier(Set.of(7.5f));

        // When
        Float value = supplier.apply(contextWithSeed(0));

        // Then
        assertThat(value).isEqualTo(7.5f);
    }

    @Test
    void everyDrawIsAMemberOfTheSuppliedSet() {
        // Given
        Set<Float> from = Set.of(1.5f, 2.5f, 3.5f);
        ChooseFloatSupplier supplier = new ChooseFloatSupplier(from);
        Context context = contextWithSeed(0);

        // When
        Float[] drawn = IntStream.range(0, 200).mapToObj(i -> supplier.apply(context)).toArray(Float[]::new);

        // Then
        assertThat(Stream.of(drawn).allMatch(from::contains)).isTrue();
    }

}
