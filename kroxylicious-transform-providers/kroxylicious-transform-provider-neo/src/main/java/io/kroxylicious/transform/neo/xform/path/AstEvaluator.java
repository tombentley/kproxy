/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.xform.path;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import io.kroxylicious.transform.neo.op.OpContext;
import io.kroxylicious.transform.neo.type.Nothing;
import io.kroxylicious.transform.neo.type.Null;

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

    /**
     * Compiles a query — a {@link Ast.Path} — into a reusable node-list evaluator that selects zero or more
     * nodes from a document.
     * @param path the (resolved, type-checked) query
     * @return a compiled query
     */
    public CompiledQuery<N> compileQuery(Ast.Path<?> path) {
        return new CompiledQuery<>(adapter, compiler.compileNodes(path));
    }

    /**
     * Compiles a value expression — a whole top-level field expression such as {@code $.value} or
     * {@code read_json($.value)} — into a reusable evaluator that produces its value against a document root.
     * @param expression the (resolved, type-checked) expression
     * @return a compiled value
     */
    public CompiledValue<N> compileValue(Ast expression) {
        return new CompiledValue<>(adapter, compiler.compileValue(expression));
    }

    /** A compiled value expression; reusable across records and threads. */
    public static final class CompiledValue<N> {

        private final TreeAdapter<N> adapter;
        private final Eval<N> eval;

        private CompiledValue(TreeAdapter<N> adapter, Eval<N> eval) {
            this.adapter = adapter;
            this.eval = eval;
        }

        /**
         * Evaluates the expression against {@code root}.
         * @param root the document root (bound to {@code $})
         * @param opContext the context passed to any function invoked by the expression
         * @return the value: a present domain value, {@link Null#INSTANCE},
         * or {@link Nothing#INSTANCE}. Callers materialising back to a
         * nullable domain (e.g. a Kafka key/value) map both unit values to a Java {@code null} at that boundary.
         */
        public Object evaluate(N root, OpContext opContext) {
            return eval.eval(new EvalContext<>(root, adapter, opContext), root);
        }
    }

    /** A compiled query; reusable across records and threads. */
    public static final class CompiledQuery<N> {

        private final TreeAdapter<N> adapter;
        private final SeqEval<N> nodes;

        private CompiledQuery(TreeAdapter<N> adapter, SeqEval<N> nodes) {
            this.adapter = adapter;
            this.nodes = nodes;
        }

        /**
         * Evaluates the query against {@code root}, passing each selected node to {@code sink} in evaluation
         * order.
         * @param root the document root (bound to {@code $})
         * @param opContext the context passed to any function invoked by the query
         * @param sink receives each selected node
         */
        public void evaluate(N root, OpContext opContext, Consumer<? super N> sink) {
            nodes.eval(new EvalContext<>(root, adapter, opContext), root, sink);
        }

        /**
         * Evaluates the query against {@code root}, collecting the selected nodes into a list.
         * @param root the document root (bound to {@code $})
         * @param opContext the context passed to any function invoked by the query
         * @return the selected nodes, in evaluation order
         */
        public List<N> evaluate(N root, OpContext opContext) {
            List<N> results = new ArrayList<>();
            evaluate(root, opContext, results::add);
            return results;
        }
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
