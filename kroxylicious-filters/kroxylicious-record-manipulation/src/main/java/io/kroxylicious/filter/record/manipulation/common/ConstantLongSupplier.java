/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.LongSupplier;

public class ConstantLongSupplier implements LongSupplier {
    private final int value;

    public ConstantLongSupplier(int value) {
        this.value = value;
    }

    @Override
    public long getAsLong() {
        return value;
    }
}
