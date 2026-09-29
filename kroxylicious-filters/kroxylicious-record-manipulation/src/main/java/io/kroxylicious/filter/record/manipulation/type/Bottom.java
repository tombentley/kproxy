/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.type;

/**
 * The bottom type (&perp;): the <em>uninhabited</em> type, with no values at all. It is (conceptually) a
 * subtype of every other type, and is the type of a statically impossible navigation — for example an
 * indexed access of an object-typed value, or a named access of an array-typed value. Because it has no
 * values, a well-typed evaluation never produces one; a {@code Bottom} in a computed type marks a branch
 * that cannot contribute any value.
 * <p>
 * {@code Bottom} must not be confused with {@link Nothing}: {@link Nothing} is a <em>unit</em> type with
 * exactly one value (the "absent" marker), whereas {@code Bottom} has zero. The two were previously
 * conflated (e.g. a JSON type system reporting {@code MissingNode} — a real, present marker — as its
 * bottom type); this type exists to keep them separate.
 */
public final class Bottom {

    private Bottom() {
    }
}
