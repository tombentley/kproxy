/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.Predicate;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A function that returns an {@code int} drawn at random from a range.
 */
public class RandomBooleanSupplier implements Predicate<OpContext> {

    /**
     * Creates an instance.
     */
    public RandomBooleanSupplier() {
    }

    @Override
    public boolean test(OpContext opContext) {
        return opContext.random().nextBoolean();
    }
}
