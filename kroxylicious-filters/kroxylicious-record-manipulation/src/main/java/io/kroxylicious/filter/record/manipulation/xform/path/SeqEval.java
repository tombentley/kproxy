/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.function.Consumer;

/**
 * A compiled (general) query that produces a node list — the {@code Seq<X>} shape of the type lattice. It
 * emits each selected node to a {@link Consumer} sink rather than materialising a collection, so existence
 * can short-circuit and large node lists need not be held in memory.
 * <p>
 * Selected nodes are always present domain values ({@code N}); absence contributes no emission (rather than
 * a {@link io.kroxylicious.filter.record.manipulation.type.Nothing} value), which is what makes existence
 * simply "did the sink receive anything".
 *
 * @param <N> the runtime tree-node type
 */
@FunctionalInterface
public interface SeqEval<N> {

    /**
     * Emits the nodes selected by this query.
     * @param ctx the per-record context (root {@code $}, adapter, op context)
     * @param current the node bound to {@code @} (the start for a relative query; ignored by an absolute one)
     * @param sink receives each selected node, in evaluation order
     */
    void eval(EvalContext<N> ctx, N current, Consumer<? super N> sink);
}
