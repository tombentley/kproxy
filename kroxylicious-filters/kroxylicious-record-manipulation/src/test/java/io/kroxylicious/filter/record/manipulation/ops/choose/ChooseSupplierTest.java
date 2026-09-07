/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.choose;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseSupplierTest {

    private static OpContext contextWithSeed(long seed) {
        return new OpContext(new Random(seed), new byte[0]);
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
        OpContext opContext = contextWithSeed(0);

        // When
        Set<String> drawn = Stream.generate(() -> supplier.apply(opContext)).limit(200).collect(Collectors.toSet());

        // Then
        assertThat(from).containsAll(drawn);
    }

}
