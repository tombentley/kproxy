/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.IntSupplier;

public class ConstantIntSupplier implements IntSupplier {
    private final int value;

    public ConstantIntSupplier(int value) {
        this.value = value;
    }

    @Override
    public int getAsInt() {
        return value;
    }
}
