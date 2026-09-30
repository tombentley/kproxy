/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.neo.xform.path;

import io.kroxylicious.filter.record.manipulation.neo.op.OpContext;

/**
 * The per-record invariants threaded through an evaluation: the document root ({@code $}), the tree
 * adapter used to navigate values, and the {@link OpContext} handed to any function ({@code FnCall})
 * invoked while evaluating.
 * <p>
 * The candidate node bound to {@code @} is <em>not</em> held here: it varies per candidate element while
 * a filter is applied, so it is passed as an explicit parameter to the compiled evaluators
 * ({@link Eval#eval}, {@link BoolEval#test}) rather than stored, which lets a single context instance be
 * reused across a whole record.
 *
 * @param <N> the runtime tree-node type (e.g. {@code JsonNode})
 * @param root the document root, bound to {@code $}
 * @param adapter navigates {@code N} values
 * @param opContext the context passed to functions invoked during evaluation
 */
public record EvalContext<N>(N root, TreeAdapter<N> adapter, OpContext opContext) {}
