/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.BiFunction;

/**
 * A single operation on an {@code byte[]}, given some {@link Context}.
 */
public interface BytesOp extends BiFunction<byte[], Context, byte[]> {
}
