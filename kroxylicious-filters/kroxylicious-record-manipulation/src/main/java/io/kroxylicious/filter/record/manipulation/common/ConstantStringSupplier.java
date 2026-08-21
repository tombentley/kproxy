/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.Function;

/**
 * A function that always returns the same {@link String}, regardless of context.
 */
public class ConstantStringSupplier implements Function<Context, String> {
    private final String value;

    /**
     * Creates an instance.
     * @param value the value to always return
     */
    public ConstantStringSupplier(String value) {
        this.value = value;
    }

    @Override
    public String apply(Context context) {
        return value;
    }
}
