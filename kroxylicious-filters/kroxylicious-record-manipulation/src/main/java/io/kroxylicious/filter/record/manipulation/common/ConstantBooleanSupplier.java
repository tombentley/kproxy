/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.Predicate;

/**
 * A function that always returns the same {@code int}, regardless of context.
 */
public class ConstantBooleanSupplier implements Predicate<Context> {
    private final boolean value;

    /**
     * Creates an instance.
     * @param value the value to always return
     */
    public ConstantBooleanSupplier(boolean value) {
        this.value = value;
    }

    @Override
    public boolean test(Context context) {
        return value;
    }
}
