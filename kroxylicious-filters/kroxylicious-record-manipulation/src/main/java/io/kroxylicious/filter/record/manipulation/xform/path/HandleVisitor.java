/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

public class HandleVisitor implements Ast.AstVisitor {

    MethodHandle handle;

    @Override
    public void visitExpr(Ast.Expr expr) {
        Ast.AstVisitor.super.visitExpr(expr);
    }

    @Override
    public void visitNull(Ast.Null literalVal) {
        handle = MethodHandles.constant(Void.TYPE, null);
    }

    @Override
    public void visitBool(Ast.Bool literalVal) {
        if (handle != null) {
            throw new IllegalStateException();
        }
        handle = MethodHandles.constant(Boolean.TYPE, literalVal.value());
    }

    @Override
    public void visitInt(Ast.Int literalVal) {
        if (handle != null) {
            throw new IllegalStateException();
        }
        handle = MethodHandles.constant(Long.TYPE, literalVal.value());
    }

    @Override
    public void visitNum(Ast.Num literalVal) {
        if (handle != null) {
            throw new IllegalStateException();
        }
        MethodHandle constant = MethodHandles.constant(Double.TYPE, literalVal.value());
        handle = MethodHandles.dropArguments(constant, 0, Object.class, OpContext.class);
    }

    @Override
    public void visitStr(Ast.Str literalVal) {
        if (handle != null) {
            throw new IllegalStateException();
        }
        MethodHandle constant = MethodHandles.constant(String.class, literalVal.value());
        handle = MethodHandles.dropArguments(constant, 0, Object.class, OpContext.class);
    }

    @Override
    public void visitBinaryOp(Ast.BinaryOp op) {
        if (handle != null) {
            throw new IllegalStateException();
        }
        op.left().accept(this);
        var left = handle;
        op.right().accept(this);
        var right = handle;
        var h = switch (op.op()) {
            case Ast.BinOp.EQ -> Cmp.EQ;
            case Ast.BinOp.NE -> Cmp.NEQ;
            default -> throw new IllegalStateException();
        };
        this.handle = MethodHandles.filterArguments(h, 0, left, right);
    }

    @Override
    public void visitDisjunction(Ast.Disjunction disjunction) {
        Ast.AstVisitor.super.visitDisjunction(disjunction);
    }

    @Override
    public void visitConjunction(Ast.Conjunction conjunction) {
        Ast.AstVisitor.super.visitConjunction(conjunction);
    }

    @Override
    public void visitNot(Ast.Not not) {
        Ast.AstVisitor.super.visitNot(not);
    }

    @Override
    public void visitFunctionExpr(Ast.FnCall fnCall) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        var op = fnCall.decl();
        MethodHandle fnHandle;
        MethodHandle d;
        try {
            d = lookup.findStatic(HandleVisitor.class, "d", MethodType.methodType(Object.class, MethodHandle.class));
            fnHandle = lookup.findVirtual(BaseTypedOp.class,
                    "apply",
                    MethodType.methodType(Object.class, Object.class, OpContext.class));
            fnHandle = fnHandle.bindTo(op);
        }
        catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        int argIndex = 0;
        for (var a : fnCall.args()) {
            a.accept(this);
            var argHandle = handle;
            this.handle = null;
            fnHandle = MethodHandles.filterArguments(fnHandle, argIndex, argHandle);
            argIndex++;
        }
        this.handle = fnHandle;
    }

    static Object d(MethodHandle handle) {
        try {
            return handle.invoke();
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // private static Class<?> erase(Type type) {
    // return erase(type, type);
    // }
    //
    // private static Class<?> erase(Type root, Type type) {
    // switch (type) {
    // case Class<?> c -> {
    // return c;
    // }
    // case ParameterizedType pt -> {
    // return erase(root, pt.getRawType());
    // }
    // case TypeVariable<?> tv -> {
    // return erase(root, tv.getBounds()[0]);
    // }
    // default -> {
    // String msg = "Could not erase type " + GenericTypeReflector.getTypeName(type);
    // if (!type.equals(root)) {
    // msg += " within type " + GenericTypeReflector.getTypeName(type);
    // }
    // throw new IllegalStateException(msg);
    // }
    // }
    // }

    @Override
    public <N> void visitSelector(Ast.Selector<N> selector) {
        Ast.AstVisitor.super.visitSelector(selector);
    }

    @Override
    public <N> void visitSegment(Ast.Segment<N> segment) {
        Ast.AstVisitor.super.visitSegment(segment);
    }

    @Override
    public <N> void visitPath(Ast.Path<N> path) {
        Ast.AstVisitor.super.visitPath(path);
    }

    private static class Cmp {
        static final MethodHandle EQ;
        static final MethodHandle NEQ;

        static {
            try {
                EQ = MethodHandles.lookup().findStatic(Cmp.class, "eq", MethodType.methodType(boolean.class, MethodHandle.class, MethodHandle.class));
                NEQ = MethodHandles.lookup().findStatic(Cmp.class, "neq", MethodType.methodType(boolean.class, MethodHandle.class, MethodHandle.class));
            }
            catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private static boolean eq(MethodHandle left, MethodHandle right) throws Throwable {
            return Objects.equals(left.invoke(), right.invoke());
        }

        private static boolean neq(MethodHandle left, MethodHandle right) throws Throwable {
            return !Objects.equals(left.invoke(), right.invoke());
        }
    }
}
