/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.BiFunction;

/**
 * A single operation on an {@link Integer}, given some {@link Context}.
 * <p>
 * See {@link StringOp} for why this needs to be its own named interface rather than a bare
 * {@code BiFunction<Integer, Context, Integer>}.
 */
public interface IntOp extends BiFunction<Integer, Context, Integer> {
}
