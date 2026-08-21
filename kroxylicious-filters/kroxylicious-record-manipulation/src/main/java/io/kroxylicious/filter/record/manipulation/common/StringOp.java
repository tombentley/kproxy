/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.BiFunction;

/**
 * A single operation on a {@link String}, given some {@link Context}.
 * <p>
 * Declared as its own interface (rather than using {@code BiFunction<String, Context, String>} directly) so
 * that instances carry a fixed, reflectable generic signature - a lambda assigned directly to a generic
 * interface type erases its type arguments at runtime, whereas one assigned to a named interface with the
 * arguments fixed does not, since the parameterization lives on the interface declaration rather than the
 * lambda. {@link ContextPipeline} relies on this to check that consecutive stages compose.
 */
public interface StringOp extends BiFunction<String, Context, String> {
}
