/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.choose;

import java.math.BigInteger;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseBigIntegerSupplierTest {

    private static OpContext contextWithSeed(long seed) {
        return new OpContext(new Random(seed), new byte[0]);
    }

    @Test
    void singleElementSetAlwaysReturnsThatElement() {
        // Given
        ChooseBigIntegerSupplier supplier = new ChooseBigIntegerSupplier(Set.of(BigInteger.valueOf(7)));

        // When
        BigInteger value = supplier.apply(BigInteger.ONE, contextWithSeed(0));

        // Then
        assertThat(value).isEqualTo(BigInteger.valueOf(7));
    }

    @Test
    void everyDrawIsAMemberOfTheSuppliedSet() {
        // Given
        Set<BigInteger> from = Set.of(BigInteger.ONE, BigInteger.TWO, BigInteger.TEN);
        ChooseBigIntegerSupplier supplier = new ChooseBigIntegerSupplier(from);
        OpContext opContext = contextWithSeed(0);

        // When
        BigInteger[] drawn = IntStream.range(0, 200).mapToObj(i -> supplier.apply(BigInteger.ONE, opContext)).toArray(BigInteger[]::new);

        // Then
        assertThat(Stream.of(drawn).allMatch(from::contains)).isTrue();
    }

}
