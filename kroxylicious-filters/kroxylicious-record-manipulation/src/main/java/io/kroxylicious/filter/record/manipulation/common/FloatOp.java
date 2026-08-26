/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.BiFunction;

/**
 * A single operation on a {@link Float}, given some {@link Context}.
 * <p>
 * See {@link StringOp} for why this needs to be its own named interface rather than a bare
 * {@code BiFunction<Float, Context, Float>}.
 */
public interface FloatOp extends BiFunction<Float, Context, Float> {
}
