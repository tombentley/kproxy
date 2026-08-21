/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Set;

/**
 * A function that returns a {@link String} drawn at random from a fixed set.
 */
public class ChooseStringSupplier extends ChooseSupplier<String> {

    /**
     * Creates an instance.
     * @param from the set of values to choose from
     */
    public ChooseStringSupplier(Set<String> from) {
        super(from);
    }
}
