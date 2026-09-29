/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import io.kroxylicious.filter.record.manipulation.type.Nothing;
import io.kroxylicious.filter.record.manipulation.type.Null;

/**
 * A compiled expression that produces a single value. This is the core of the evaluator: an
 * {@link io.kroxylicious.filter.record.manipulation.xform.path.EvalCompiler} turns each AST node into an
 * {@code Eval} (or a {@link BoolEval}) once, so the per-record cost is just a closure call.
 * <p>
 * The returned value is a value of the node's lattice type: a present domain value (an {@code N}, or a
 * boxed scalar such as {@link Long}/{@link String}), the canonical present-null {@link Null#INSTANCE}, or
 * the canonical absence {@link Nothing#INSTANCE}. It is <em>never</em> Java {@code null}: the evaluator's
 * internal algebra is Java-null-free, so callers never have to null-check the result.
 *
 * @param <N> the runtime tree-node type
 */
@FunctionalInterface
public interface Eval<N> {

    /**
     * Evaluates this expression.
     * @param ctx the per-record context (root {@code $}, adapter, op context)
     * @param current the node bound to {@code @} (the candidate element inside a filter; the root otherwise)
     * @return a present value, {@link Null#INSTANCE}, or {@link Nothing#INSTANCE}; never Java {@code null}
     */
    Object eval(EvalContext<N> ctx, N current);
}
