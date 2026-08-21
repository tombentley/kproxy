/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseSupplierTest {

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void singleElementSetAlwaysReturnsThatElement() {
        // Given
        ChooseSupplier<String> supplier = new ChooseSupplier<>(Set.of("only"));

        // When
        String value = supplier.apply(contextWithSeed(0));

        // Then
        assertThat(value).isEqualTo("only");
    }

    @Test
    void everyDrawIsAMemberOfTheSuppliedSet() {
        // Given
        Set<String> from = Set.of("a", "b", "c");
        ChooseSupplier<String> supplier = new ChooseSupplier<>(from);
        Context context = contextWithSeed(0);

        // When
        Set<String> drawn = Stream.generate(() -> supplier.apply(context)).limit(200).collect(Collectors.toSet());

        // Then
        assertThat(from).containsAll(drawn);
    }

}
