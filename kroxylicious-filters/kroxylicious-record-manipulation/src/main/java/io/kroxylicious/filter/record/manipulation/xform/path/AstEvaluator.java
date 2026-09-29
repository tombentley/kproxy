/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.Objects;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * Compiles resolved, type-checked {@link Ast} expressions into reusable, thread-safe evaluators over a
 * given {@link TreeAdapter}. Compilation happens once; the returned objects can be applied to many records.
 * <p>
 * Currently exposes filter-predicate compilation (the body of a {@code [?...]} selector). Whole-path and
 * whole-transformation evaluation are added as the node-list and record-transform support land.
 *
 * @param <N> the runtime tree-node type
 */
public final class AstEvaluator<N> {

    private final TreeAdapter<N> adapter;
    private final EvalCompiler<N> compiler = new EvalCompiler<>();

    public AstEvaluator(TreeAdapter<N> adapter) {
        this.adapter = Objects.requireNonNull(adapter);
    }

    /**
     * Compiles a filter predicate — the logical expression inside a {@code [?...]} selector — into a
     * reusable test of a candidate node ({@code @}) against a document root ({@code $}).
     * @param expression the (resolved, type-checked) filter body
     * @return a compiled predicate
     */
    public CompiledPredicate<N> compilePredicate(Ast expression) {
        return new CompiledPredicate<>(adapter, compiler.compileBool(expression));
    }

    /** A compiled filter predicate; reusable across records and threads. */
    public static final class CompiledPredicate<N> {

        private final TreeAdapter<N> adapter;
        private final BoolEval<N> bool;

        private CompiledPredicate(TreeAdapter<N> adapter, BoolEval<N> bool) {
            this.adapter = adapter;
            this.bool = bool;
        }

        /**
         * Tests whether {@code candidate} (bound to {@code @}) satisfies the predicate, with {@code root}
         * bound to {@code $}.
         * @param root the document root
         * @param candidate the candidate node
         * @param opContext the context passed to any function invoked by the predicate
         * @return the predicate's truth value
         */
        public boolean test(N root, N candidate, OpContext opContext) {
            return bool.test(new EvalContext<>(root, adapter, opContext), candidate);
        }
    }
}
