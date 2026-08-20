/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

public class ChooseSupplier<T> implements Supplier<T> {
    private final Random prng;
    private final Object[] values;

    public ChooseSupplier(Random prng, Set<T> from) {
        this.prng = prng;
        values = from.toArray(Object[]::new);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        return (T) values[prng.nextInt(0, values.length)];
    }
}
