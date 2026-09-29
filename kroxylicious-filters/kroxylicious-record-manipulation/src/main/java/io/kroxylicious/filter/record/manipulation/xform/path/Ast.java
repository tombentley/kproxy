/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;

import edu.umd.cs.findbugs.annotations.Nullable;

abstract class Ast {

    private ParserRuleContext tree;
    private Type type;

    public Ast setTree(ParserRuleContext tree) {
        this.tree = tree;
        return this;
    }

    public ParserRuleContext tree() {
        return tree;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type type() {
        return type;
    }

    public abstract void accept(AstVisitor visitor);

    public enum BinOp {
        AND,
        OR,
        NOT,
        EQ,
        NE,
        LT,
        LE,
        GT,
        GE
    }

    public interface AstVisitor {

        default void visitExpr(Expr expr) {
            expr.expr().accept(this);
        }

        default void visitNull(Null literalVal) {
        }

        default void visitBool(Bool literalVal) {
        }

        default void visitInt(Int literalVal) {
        }

        default void visitNum(Num literalVal) {
        }

        default void visitStr(Str literalVal) {
        }

        default void visitBinaryOp(BinaryOp op) {
            op.left().accept(this);
            op.right().accept(this);
        }

        default void visitDisjunction(Disjunction disjunction) {
            disjunction.disjunctees().forEach(s -> s.accept(this));
        }

        default void visitConjunction(Conjunction conjunction) {
            conjunction.conjunctees().forEach(s -> s.accept(this));
        }

        default void visitNot(Not not) {
            not.negatee().accept(this);
        }

        default void visitFunctionExpr(FnCall fnCall) {
            fnCall.args().forEach(s -> s.accept(this));
        }

        default <N> void visitSelector(Selector<N> selector) {
        }

        /**
         * Visits a filter selector ({@code ?<expr>}), descending into its sub-expression by default. This is the
         * hook by which visitors reach (and so resolve/type-check) the filter's {@link Selector.Filter#expression() expression}.
         */
        default <N> void visitFilter(Selector.Filter<N> filter) {
            filter.expression().accept(this);
        }

        default <N> void visitSegment(Segment<N> segment) {
            if (segment instanceof Segment.Child<N> c) {
                c.selectors().forEach(s -> s.accept(this));
            }
            if (segment instanceof Segment.Descendant<N> c) {
                c.selectors().forEach(s -> s.accept(this));
            }
        }

        default <N> void visitPath(Path<N> path) {
            path.segments.forEach(s -> s.accept(this));
        }

    }

    public static final class Bool extends Ast {
        private final boolean value;

        public Bool(boolean value) {
            this.value = value;
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitBool(this);
        }

        public boolean value() {
            return value;
        }

        @Override
        public String toString() {
            return "Bool[" +
                    "value=" + value + ']';
        }

    }

    public static final class Int extends Ast {
        private final long value;

        public Int(long value) {
            this.value = value;
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitInt(this);
        }

        public long value() {
            return value;
        }
    }

    public static final class Null extends Ast {
        public Null() {
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitNull(this);
        }

    }

    public static final class Num extends Ast {
        private final double value;

        public Num(double value) {
            this.value = value;
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitNum(this);
        }

        public double value() {
            return value;
        }

    }

    public static final class Str extends Ast {
        private final String value;

        public Str(String value) {
            this.value = value;
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitStr(this);
        }

        public String value() {
            return value;
        }

    }

    public static class BinaryOp extends Ast {
        private final BinOp op;
        private final Ast left;
        private final Ast right;

        public BinaryOp(Ast left, BinOp op, Ast right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        public Ast left() {
            return left;
        }

        public BinOp op() {
            return op;
        }

        public Ast right() {
            return right;
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitBinaryOp(this);
        }

    }

    public static final class Conjunction extends Ast {
        private final List<Ast> conjunctees;

        public Conjunction(List<Ast> conjunctees) {
            this.conjunctees = conjunctees;
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitConjunction(this);
        }

        public List<Ast> conjunctees() {
            return conjunctees;
        }

    }

    public static final class Disjunction extends Ast {
        private final List<Ast> disjunctees;

        public Disjunction(List<Ast> disjunctees) {
            this.disjunctees = disjunctees;
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitDisjunction(this);
        }

        public List<Ast> disjunctees() {
            return disjunctees;
        }
    }

    public static final class Not extends Ast {

        private final Ast negatee;

        public Not(Ast negatee) {
            this.negatee = negatee;
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitNot(this);
        }

        public Ast negatee() {
            return negatee;
        }
    }

    public static final class FnCall extends Ast {
        private final String fnName;
        private final List<Ast> args;
        private BaseTypedOp<?, ?> op;

        public FnCall(String fnName,
                      List<Ast> args) {
            this.fnName = fnName;
            this.args = args;
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitFunctionExpr(this);
        }

        public String fnName() {
            return fnName;
        }

        public List<Ast> args() {
            return args;
        }

        public void setDecl(BaseTypedOp<?, ?> op) {
            this.op = op;
        }

        public BaseTypedOp<?, ?> decl() {
            return op;
        }
    }

    public abstract static class Selector<N> extends Ast {
        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitSelector(this);
        }

        public static final class Name<N> extends Selector<N> {
            private final String name;

            public Name(String name) {
                this.name = name;
            }

            boolean matches(String n) {
                return name.equals(n);
            }

            @Override
            public String toString() {
                return name;
            }

            public String name() {
                return name;
            }

        }

        public static final class Children<N> extends Selector<N> {
            public Children() {
            }

            @Override
            public String toString() {
                return "*";
            }

        }

        public static final class Index<N> extends Selector<N> {
            private final int index;

            public Index(int index) {
                this.index = index;
            }

            /** Whether the selector picks index {@code i} of an array of length {@code len}; negatives count from the end (RFC 9535 §2.3.1). */
            boolean matches(int i, int len) {
                return (index < 0 ? len + index : index) == i;
            }

            @Override
            public String toString() {
                return Integer.toString(index);
            }

            public int index() {
                return index;
            }
        }

        public static final class Slice<N> extends Selector<N> {
            @Nullable
            private final Integer start;
            @Nullable
            private final Integer end;
            private final int step;

            public Slice(@Nullable Integer start, @Nullable Integer end, int step) {
                this.start = start;
                this.end = end;
                this.step = step;
            }

            Slice(@Nullable Integer start, @Nullable Integer end) {
                this(start, end, 1);
            }

            /** Whether index {@code i} of an array of length {@code len} falls in the slice (RFC 9535 §2.3.4). */
            boolean matches(int i, int len) {
                if (step == 0) {
                    return false;
                }
                boolean forwards = step > 0;
                int lo = forwards ? 0 : -1;
                int hi = forwards ? len : len - 1;
                int anchor = clamp(resolve(start, forwards ? 0 : len - 1, len), lo, hi);
                int limit = clamp(resolve(end, forwards ? len : -len - 1, len), lo, hi);
                return (i - anchor) % step == 0
                        && (forwards ? anchor <= i && i < limit : limit < i && i <= anchor);
            }

            /** Supplies the default when a bound is omitted, then maps a negative bound to an offset from {@code len}. */
            private static int resolve(@Nullable Integer bound, int dflt, int len) {
                int b = bound == null ? dflt : bound;
                return b < 0 ? len + b : b;
            }

            private static int clamp(int v, int lo, int hi) {
                return Math.min(Math.max(v, lo), hi);
            }

            @Override
            public String toString() {
                return (start == null ? "" : start.toString())
                        + ":" + (end == null ? "" : end.toString())
                        + (step == 1 ? "" : ":" + step);
            }

            @Nullable
            public Integer start() {
                return start;
            }

            @Nullable
            public Integer end() {
                return end;
            }

            public int step() {
                return step;
            }
        }

        /**
         * A filter selector {@code ?<expr>}. The {@link #expression() expression} is the parsed logical
         * sub-tree; within it a {@code @}-rooted {@link Path} refers to the candidate node and a {@code $}-rooted
         * one to the document root (RFC 9535 §2.3.5).
         */
        static final class Filter<N> extends Selector<N> {
            private final Ast expression;

            Filter(Ast expression) {
                this.expression = expression;
            }

            /** The parsed logical expression that a candidate node must satisfy to be selected. */
            Ast expression() {
                return expression;
            }

            @Override
            public void accept(AstVisitor visitor) {
                visitor.visitFilter(this);
            }

            @Override
            public String toString() {
                return "?" + expression;
            }
        }

    }

    public abstract static class Segment<N> extends Ast {
        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitSegment(this);
        }

        public static final class Child<N> extends Segment<N> {
            private final List<Selector<N>> selectors;

            public Child(List<Selector<N>> selectors) {
                this.selectors = selectors;
            }

            Child(Selector<N> selector) {
                this(List.of(selector));
            }

            @Override
            public String toString() {
                return selectors.toString();
            }

            public List<Selector<N>> selectors() {
                return selectors;
            }
        }

        public static final class Descendant<N> extends Segment<N> {
            private final List<Selector<N>> selectors;

            public Descendant(List<Selector<N>> selectors) {
                this.selectors = selectors;
            }

            Descendant(Selector<N> selector) {
                this(List.of(selector));
            }

            @Override
            public String toString() {
                return ".." +
                        selectors;
            }

            public List<Selector<N>> selectors() {
                return selectors;
            }
        }

    }

    public static final class Path<N> extends Ast {
        private final Identifier identifier;
        private final List<Segment<N>> segments;
        private final Consumer<N> consumer;

        public Path(Identifier identifier, List<Segment<N>> segments, Consumer<N> consumer) {
            this.identifier = identifier;
            this.segments = segments;
            this.consumer = consumer;
        }

        public Path(Identifier identifier, Segment<N> segment, Consumer<N> consumer) {
            this(identifier, List.of(segment), consumer);
        }

        @Override
        public String toString() {
            return identifier.symbol +
                    segments.stream().map(Object::toString).collect(Collectors.joining("")) +
                    ", consumer=" + consumer +
                    '}';
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitPath(this);
        }

        public Identifier identifier() {
            return identifier;
        }

        public List<Segment<N>> segments() {
            return segments;
        }

        public Consumer<N> consumer() {
            return consumer;
        }

    }

    public static final class Expr extends Ast {

        private final Ast expr;

        public Expr(Ast expr) {
            this.expr = expr;
        }

        public Ast expr() {
            return expr;
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visitExpr(this);
        }
    }
}
