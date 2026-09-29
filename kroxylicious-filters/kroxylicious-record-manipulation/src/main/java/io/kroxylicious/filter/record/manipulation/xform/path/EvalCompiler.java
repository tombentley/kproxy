/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.List;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.type.Nothing;
import io.kroxylicious.filter.record.manipulation.type.Null;

import tools.jackson.databind.JsonNode;

/**
 * Compiles a resolved, type-checked {@link Ast} sub-tree into a reusable closure ({@link Eval} /
 * {@link BoolEval}) once, so per-record evaluation is free of node-kind dispatch and reflection.
 * <p>
 * This stage supports the value algebra and the filter-predicate language: literals, singular queries
 * ({@code @.a.b}, {@code $.x}, i.e. name/index segments), comparisons, conjunction/disjunction/negation,
 * existence tests, and function calls. General (multi-node) queries — wildcards, slices, descendants,
 * unions of selectors and filters embedded in a path — are not yet compiled here; those are the province
 * of the forthcoming {@code SeqEval} node-list support and throw {@link UnsupportedOperationException} for
 * now.
 *
 * @param <N> the runtime tree-node type
 */
class EvalCompiler<N> {

    /** Compiles {@code ast} to a value-producing evaluator. */
    Eval<N> compileValue(Ast ast) {
        if (ast instanceof Ast.Int i) {
            Long v = i.value();
            return (ctx, current) -> v;
        }
        if (ast instanceof Ast.Num n) {
            Double v = n.value();
            return (ctx, current) -> v;
        }
        if (ast instanceof Ast.Str s) {
            String v = s.value();
            return (ctx, current) -> v;
        }
        if (ast instanceof Ast.Bool b) {
            Boolean v = b.value();
            return (ctx, current) -> v;
        }
        if (ast instanceof Ast.Null) {
            return (ctx, current) -> Null.INSTANCE;
        }
        if (ast instanceof Ast.Path<?> path) {
            return compilePath(path);
        }
        if (ast instanceof Ast.FnCall fn) {
            return compileFnValue(fn);
        }
        throw new UnsupportedOperationException("cannot evaluate as a value: " + ast.getClass().getSimpleName());
    }

    /** Compiles {@code ast} to a logical (boolean) evaluator. */
    BoolEval<N> compileBool(Ast ast) {
        if (ast instanceof Ast.BinaryOp op) {
            Ast.BinOp binOp = op.op();
            Eval<N> left = compileValue(op.left());
            Eval<N> right = compileValue(op.right());
            return (ctx, current) -> RfcComparisons.compare(binOp, left.eval(ctx, current), right.eval(ctx, current));
        }
        if (ast instanceof Ast.Conjunction conjunction) {
            List<BoolEval<N>> parts = conjunction.conjunctees().stream().map(this::compileBool).toList();
            return (ctx, current) -> {
                for (BoolEval<N> part : parts) {
                    if (!part.test(ctx, current)) {
                        return false;
                    }
                }
                return true;
            };
        }
        if (ast instanceof Ast.Disjunction disjunction) {
            List<BoolEval<N>> parts = disjunction.disjunctees().stream().map(this::compileBool).toList();
            return (ctx, current) -> {
                for (BoolEval<N> part : parts) {
                    if (part.test(ctx, current)) {
                        return true;
                    }
                }
                return false;
            };
        }
        if (ast instanceof Ast.Not not) {
            BoolEval<N> inner = compileBool(not.negatee());
            return (ctx, current) -> !inner.test(ctx, current);
        }
        if (ast instanceof Ast.Path<?> path) {
            // Existence test: the query selects at least one node.
            Eval<N> nav = compilePath(path);
            return (ctx, current) -> nav.eval(ctx, current) != Nothing.INSTANCE;
        }
        if (ast instanceof Ast.FnCall fn) {
            Eval<N> value = compileFnValue(fn);
            return (ctx, current) -> asBoolean(value.eval(ctx, current));
        }
        throw new UnsupportedOperationException("cannot evaluate as a logical expression: " + ast.getClass().getSimpleName());
    }

    private Eval<N> compilePath(Ast.Path<?> path) {
        Identifier identifier = path.identifier();
        List<? extends Ast.Segment<?>> segments = path.segments();
        return (ctx, current) -> {
            Object start = identifier == Identifier.ROOT ? ctx.root() : current;
            return navigate(ctx, start, segments);
        };
    }

    private Object navigate(EvalContext<N> ctx, Object start, List<? extends Ast.Segment<?>> segments) {
        Object curr = start;
        for (Ast.Segment<?> segment : segments) {
            if (curr == Nothing.INSTANCE) {
                return Nothing.INSTANCE;
            }
            @SuppressWarnings("unchecked")
            N node = (N) curr;
            curr = step(ctx, node, segment);
        }
        return curr;
    }

    private Object step(EvalContext<N> ctx, N node, Ast.Segment<?> segment) {
        if (!(segment instanceof Ast.Segment.Child<?> child)) {
            throw new UnsupportedOperationException("descendant segments are not yet evaluated (deferred to node-list support)");
        }
        List<? extends Ast.Selector<?>> selectors = child.selectors();
        if (selectors.size() != 1) {
            throw new UnsupportedOperationException("multi-selector segments are not yet evaluated (deferred to node-list support)");
        }
        Ast.Selector<?> selector = selectors.get(0);
        TreeAdapter<N> adapter = ctx.adapter();
        if (selector instanceof Ast.Selector.Name<?> name) {
            if (!adapter.isObject(node)) {
                return Nothing.INSTANCE;
            }
            for (Prop<N> prop : adapter.objectProperties(node)) {
                if (prop.propertyName().equals(name.name())) {
                    return prop.propertyValue();
                }
            }
            return Nothing.INSTANCE;
        }
        if (selector instanceof Ast.Selector.Index<?> index) {
            if (!adapter.isArray(node)) {
                return Nothing.INSTANCE;
            }
            int length = adapter.arrayLength(node);
            int i = index.index() < 0 ? length + index.index() : index.index();
            if (i < 0 || i >= length) {
                return Nothing.INSTANCE;
            }
            return adapter.arrayItem(node, i);
        }
        throw new UnsupportedOperationException(
                selector.getClass().getSimpleName() + " selectors are not yet evaluated (deferred to node-list support)");
    }

    private Eval<N> compileFnValue(Ast.FnCall fn) {
        BaseTypedOp<?, ?> op = fn.decl();
        Eval<N> argEval = fn.args().isEmpty()
                ? (ctx, current) -> Nothing.INSTANCE
                : compileValue(fn.args().getFirst());
        return (ctx, current) -> {
            Object arg = argEval.eval(ctx, current);
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object result = ((BaseTypedOp) op).apply(arg, ctx.opContext());
            // TODO enforce the op-null contract: a non-optional op must not return null. Until optionality is
            // threaded through the type checker, defensively map a null result to Nothing to keep the algebra
            // Java-null-free.
            return result == null ? Nothing.INSTANCE : result;
        };
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof JsonNode node && node.isBoolean()) {
            return node.booleanValue();
        }
        return false;
    }
}
