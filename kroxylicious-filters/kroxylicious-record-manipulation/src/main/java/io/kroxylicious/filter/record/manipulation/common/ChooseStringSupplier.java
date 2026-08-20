/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;

/**
 * A supplier that returns a {@link String} drawn at random from a fixed set.
 */
public class ChooseStringSupplier extends ChooseSupplier<String> {

    /**
     * Creates a supplier.
     * @param prng the source of randomness
     * @param from the set of values to choose from
     */
    public ChooseStringSupplier(Random prng, Set<String> from) {
        super(prng, from);
    }
}
