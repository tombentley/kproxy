/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.Set;

public class ChooseStringSupplier extends ChooseSupplier<String> {

    public ChooseStringSupplier(Random prng, Set<String> from) {
        super(prng, from);
    }
}
