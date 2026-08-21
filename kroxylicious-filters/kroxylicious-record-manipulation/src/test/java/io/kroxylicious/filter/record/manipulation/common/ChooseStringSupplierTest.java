/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseStringSupplierTest {

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), new byte[0]);
    }

    @Test
    void singleElementSetAlwaysReturnsThatElement() {
        // Given
        ChooseStringSupplier supplier = new ChooseStringSupplier(Set.of("only"));

        // When
        String value = supplier.apply(contextWithSeed(0));

        // Then
        assertThat(value).isEqualTo("only");
    }

    @Test
    void everyDrawIsAMemberOfTheSuppliedSet() {
        // Given
        Set<String> from = Set.of("a", "b", "c");
        ChooseStringSupplier supplier = new ChooseStringSupplier(from);
        Context context = contextWithSeed(0);

        // When
        boolean allMembers = Stream.generate(() -> supplier.apply(context)).limit(200).allMatch(from::contains);

        // Then
        assertThat(allMembers).isTrue();
    }

}
