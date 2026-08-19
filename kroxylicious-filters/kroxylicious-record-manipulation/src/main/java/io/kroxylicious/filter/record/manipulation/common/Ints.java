/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;
import java.util.function.IntSupplier;

public class Ints {

    private final Random prng = new Random();

    public IntSupplier value(int value) {
        return () -> value;
    }

    public IntSupplier choose(Set<Integer> from) {
        var values = from.stream().mapToInt(i -> i).toArray();
        return () -> values[prng.nextInt(0, from.size())];
    }

    public IntSupplier random(int min, int max) {
        return () -> prng.nextInt(min, max + 1);
    }
}
