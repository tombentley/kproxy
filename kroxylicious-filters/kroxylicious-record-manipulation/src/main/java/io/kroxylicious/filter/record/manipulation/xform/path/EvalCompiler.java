/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.List;
import java.util.function.Consumer;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.type.Nothing;
import io.kroxylicious.filter.record.manipulation.type.Null;

import tools.jackson.databind.JsonNode;

/**
 * Compiles a resolved, type-checked {@link Ast} sub-tree into a reusable closure once, so per-record
 * evaluation is free of node-kind dispatch and reflection. Produces:
 * <ul>
 *   <li>{@link Eval} — a value (literal, singular query, function call);</li>
 *   <li>{@link BoolEval} — a truth value (comparison, {@code &&}/{@code ||}/{@code !}, existence test,
 *       boolean function);</li>
 *   <li>{@link SeqEval} — a node list (a general query, over all selectors and segments).</li>
 * </ul>
 * A query is compiled by {@link #compileNodes}; a singular query used as a value takes the sole node (or
 * {@link Nothing}), and an existence test asks whether the node list is non-empty — both are coercions over
 * the one node-list engine. Node ordering follows RFC 9535 (per-selector for a union, and the step
 * direction for a slice, so a negative-step slice is reversed).
 *
 * @param <N> the runtime tree-node type
 */
class EvalCompiler<N> {

    /** A compiled segment or selector: applies to a single {@code node}, emitting the nodes it selects. */
    @FunctionalInterface
    private interface Step<N> {
        void apply(EvalContext<N> ctx, N node, Consumer<? super N> sink);
    }

    /** Compiles {@code ast} to a value-producing evaluator. */
    Eval<N> compileValue(Ast ast) {
        if (ast instanceof Ast.Expr expr) {
            return compileValue(expr.expr());
        }
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
            return singular(compileNodes(path));
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
            SeqEval<N> nodes = compileNodes(path);
            return (ctx, current) -> exists(nodes, ctx, current);
        }
        if (ast instanceof Ast.FnCall fn) {
            Eval<N> value = compileFnValue(fn);
            return (ctx, current) -> asBoolean(value.eval(ctx, current));
        }
        throw new UnsupportedOperationException("cannot evaluate as a logical expression: " + ast.getClass().getSimpleName());
    }

    /** Compiles a query into a node-list producer. */
    SeqEval<N> compileNodes(Ast.Path<?> path) {
        Identifier identifier = path.identifier();
        List<Step<N>> steps = path.segments().stream().map(this::compileSegment).toList();
        return (ctx, current, sink) -> {
            N start = identifier == Identifier.ROOT ? ctx.root() : current;
            run(ctx, steps, 0, start, sink);
        };
    }

    private void run(EvalContext<N> ctx, List<Step<N>> steps, int index, N node, Consumer<? super N> sink) {
        if (index == steps.size()) {
            sink.accept(node);
            return;
        }
        steps.get(index).apply(ctx, node, next -> run(ctx, steps, index + 1, next, sink));
    }

    private Step<N> compileSegment(Ast.Segment<?> segment) {
        if (segment instanceof Ast.Segment.Child<?> child) {
            List<Step<N>> selectors = child.selectors().stream().map(this::compileSelector).toList();
            return (ctx, node, sink) -> {
                for (Step<N> selector : selectors) {
                    selector.apply(ctx, node, sink);
                }
            };
        }
        if (segment instanceof Ast.Segment.Descendant<?> descendant) {
            List<Step<N>> selectors = descendant.selectors().stream().map(this::compileSelector).toList();
            return (ctx, node, sink) -> descend(ctx, node, selectors, sink);
        }
        throw new IllegalStateException("unknown segment: " + segment.getClass().getSimpleName());
    }

    /** Descendant-or-self, pre-order: apply the selectors to {@code node}, then recurse into its children. */
    private void descend(EvalContext<N> ctx, N node, List<Step<N>> selectors, Consumer<? super N> sink) {
        for (Step<N> selector : selectors) {
            selector.apply(ctx, node, sink);
        }
        TreeAdapter<N> adapter = ctx.adapter();
        if (adapter.isObject(node)) {
            for (Prop<N> prop : adapter.objectProperties(node)) {
                descend(ctx, prop.propertyValue(), selectors, sink);
            }
        }
        else if (adapter.isArray(node)) {
            int length = adapter.arrayLength(node);
            for (int i = 0; i < length; i++) {
                descend(ctx, adapter.arrayItem(node, i), selectors, sink);
            }
        }
    }

    private Step<N> compileSelector(Ast.Selector<?> selector) {
        if (selector instanceof Ast.Selector.Name<?> name) {
            String propertyName = name.name();
            return (ctx, node, sink) -> {
                TreeAdapter<N> adapter = ctx.adapter();
                if (adapter.isObject(node)) {
                    for (Prop<N> prop : adapter.objectProperties(node)) {
                        if (prop.propertyName().equals(propertyName)) {
                            sink.accept(prop.propertyValue());
                            return;
                        }
                    }
                }
            };
        }
        if (selector instanceof Ast.Selector.Index<?> index) {
            int declared = index.index();
            return (ctx, node, sink) -> {
                TreeAdapter<N> adapter = ctx.adapter();
                if (adapter.isArray(node)) {
                    int length = adapter.arrayLength(node);
                    int i = declared < 0 ? length + declared : declared;
                    if (i >= 0 && i < length) {
                        sink.accept(adapter.arrayItem(node, i));
                    }
                }
            };
        }
        if (selector instanceof Ast.Selector.Children<?>) {
            return (ctx, node, sink) -> {
                TreeAdapter<N> adapter = ctx.adapter();
                if (adapter.isObject(node)) {
                    for (Prop<N> prop : adapter.objectProperties(node)) {
                        sink.accept(prop.propertyValue());
                    }
                }
                else if (adapter.isArray(node)) {
                    int length = adapter.arrayLength(node);
                    for (int i = 0; i < length; i++) {
                        sink.accept(adapter.arrayItem(node, i));
                    }
                }
            };
        }
        if (selector instanceof Ast.Selector.Slice<?> slice) {
            return compileSlice(slice);
        }
        if (selector instanceof Ast.Selector.Filter<?> filter) {
            BoolEval<N> predicate = compileBool(filter.expression());
            return (ctx, node, sink) -> {
                TreeAdapter<N> adapter = ctx.adapter();
                if (adapter.isObject(node)) {
                    for (Prop<N> prop : adapter.objectProperties(node)) {
                        N candidate = prop.propertyValue();
                        if (predicate.test(ctx, candidate)) {
                            sink.accept(candidate);
                        }
                    }
                }
                else if (adapter.isArray(node)) {
                    int length = adapter.arrayLength(node);
                    for (int i = 0; i < length; i++) {
                        N candidate = adapter.arrayItem(node, i);
                        if (predicate.test(ctx, candidate)) {
                            sink.accept(candidate);
                        }
                    }
                }
            };
        }
        throw new IllegalStateException("unknown selector: " + selector.getClass().getSimpleName());
    }

    private Step<N> compileSlice(Ast.Selector.Slice<?> slice) {
        return (ctx, node, sink) -> {
            TreeAdapter<N> adapter = ctx.adapter();
            if (!adapter.isArray(node)) {
                return;
            }
            int length = adapter.arrayLength(node);
            if (slice.step() >= 0) {
                for (int i = 0; i < length; i++) {
                    if (slice.matches(i, length)) {
                        sink.accept(adapter.arrayItem(node, i));
                    }
                }
            }
            else {
                // Negative step: emit in reverse (RFC 9535 §2.3.4.2 ordering).
                for (int i = length - 1; i >= 0; i--) {
                    if (slice.matches(i, length)) {
                        sink.accept(adapter.arrayItem(node, i));
                    }
                }
            }
        };
    }

    /** Coerces a query to a single value: its sole node, or {@link Nothing#INSTANCE} if it selects none. */
    private Eval<N> singular(SeqEval<N> nodes) {
        return (ctx, current) -> {
            Object[] holder = { Nothing.INSTANCE };
            nodes.eval(ctx, current, node -> {
                if (holder[0] == Nothing.INSTANCE) {
                    holder[0] = node;
                }
            });
            return holder[0];
        };
    }

    private boolean exists(SeqEval<N> nodes, EvalContext<N> ctx, N current) {
        boolean[] found = { false };
        nodes.eval(ctx, current, node -> found[0] = true);
        return found[0];
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
