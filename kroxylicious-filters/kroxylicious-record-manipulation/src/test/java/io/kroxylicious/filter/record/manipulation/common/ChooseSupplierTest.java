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

    @Test
    void singleElementSetAlwaysReturnsThatElement() {
        // Given
        ChooseSupplier<String> supplier = new ChooseSupplier<>(new Random(), Set.of("only"));

        // When
        String value = supplier.get();

        // Then
        assertThat(value).isEqualTo("only");
    }

    @Test
    void everyDrawIsAMemberOfTheSuppliedSet() {
        // Given
        Set<String> from = Set.of("a", "b", "c");
        ChooseSupplier<String> supplier = new ChooseSupplier<>(new Random(0), from);

        // When
        Set<String> drawn = Stream.generate(supplier).limit(200).collect(Collectors.toSet());

        // Then
        assertThat(from).containsAll(drawn);
    }

}
