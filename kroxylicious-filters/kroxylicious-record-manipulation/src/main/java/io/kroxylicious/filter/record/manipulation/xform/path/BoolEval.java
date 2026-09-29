/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

/**
 * A compiled logical expression that produces a {@code boolean}. This is an unboxed-boolean specialisation
 * of {@link Eval} for comparison, conjunction/disjunction/negation and existence-test nodes — an
 * optimisation and ergonomic convenience, not a distinct type category ({@code boolean} is an ordinary
 * value type in the lattice).
 * <p>
 * Per RFC 9535 a logical/comparison expression never throws: a comparison of incompatible types simply
 * yields {@code false}.
 *
 * @param <N> the runtime tree-node type
 */
@FunctionalInterface
public interface BoolEval<N> {

    /**
     * Evaluates this logical expression.
     * @param ctx the per-record context (root {@code $}, adapter, op context)
     * @param current the node bound to {@code @} (the candidate element inside a filter)
     * @return the truth value
     */
    boolean test(EvalContext<N> ctx, N current);
}
