/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;
import java.util.List;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.NaryOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.ops.common.ComposedOp;

public class OpBuildingVisitor implements Ast.AstVisitor {

    @Override
    public void visitExpr(Ast.Expr expr) {
        Ast.AstVisitor.super.visitExpr(expr);
    }

    @Override
    public void visitNull(Ast.Null aNull) {
        Ast.AstVisitor.super.visitNull(aNull);
        BaseTypedOp.of(Void.class, Void.class, (value, opContext) -> null);
    }

    @Override
    public void visitBool(Ast.Bool bool) {
        Ast.AstVisitor.super.visitBool(bool);
        MethodHandles.constant(Boolean.TYPE, bool.value());
        BaseTypedOp.of(Boolean.TYPE, Boolean.TYPE, (value, opContext) -> bool.value());
    }

    @Override
    public void visitInt(Ast.Int anInt) {
        Ast.AstVisitor.super.visitInt(anInt);
        MethodHandles.constant(Long.TYPE, anInt.value());
        BaseTypedOp.of(Long.TYPE, Long.TYPE, (value, opContext) -> anInt.value());
    }

    @Override
    public void visitNum(Ast.Num num) {
        Ast.AstVisitor.super.visitNum(num);
        MethodHandles.constant(Double.TYPE, num.value());
        BaseTypedOp.of(Double.TYPE, Double.TYPE, (value, opContext) -> num.value());
    }

    @Override
    public void visitStr(Ast.Str str) {
        Ast.AstVisitor.super.visitStr(str);
        MethodHandles.constant(String.class, str.value());
        BaseTypedOp.of(String.class, String.class, (value, opContext) -> str.value());
    }

    @Override
    public void visitBinaryOp(Ast.BinaryOp op) {
        Ast.AstVisitor.super.visitBinaryOp(op);
        if (op.op() == Ast.BinOp.EQ) {
            var mh = MethodHandles.lookup().findVirtual(Object.class, "equals", MethodType.methodType(Boolean.TYPE, Object.class));
            mh = mh.bindTo(op.left());

        }
        // TODO this should be roughly the same as Not
        // but BaseTypedOp currently only has 1 parameter (ignoring the context)
        // so we cannot represent a bin op as a BaseTypedOp
        NaryOp<Boolean> op2 = switch (op.op()) {
            case EQ -> new NaryOp<Boolean>() {

                @Override
                public List<Type> inputTypes() {
                    return List.of(op.left().type(), op.right().type());
                }

                @Override
                public Type outputType() {
                    return Boolean.TYPE;
                }

                @Override
                public Boolean applyN(Object[] params, OpContext opContext) {
                    var leftVal = op.left().decl().apply(params[0], opContext);
                    var rightVal = op.right().decl().apply(params[0], opContext);
                    return leftVal.equals(rightVal);
                }
            };
            default -> throw new IllegalArgumentException("Unsupported binary op: " + op.op());
        };
        op.setDecl(op2);
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
        NaryOp<Boolean> op = not.negatee().decl();
        BaseTypedOp.of(Object.class, Boolean.TYPE, (value, opContext) -> !op.applyN(new Object[]{value}, opContext));
    }

    @Override
    public void visitFunctionExpr(Ast.FnCall fnCall) {
        Ast.AstVisitor.super.visitFunctionExpr(fnCall);
        NaryOp decl = fnCall.decl();
        // TODO we need to run the visitor over trees like
        // foo(bar($)
        // and compose the decl for foo with the decl for bar.
        BaseTypedOp bar = null;
        new ComposedOp<>(bar, decl);
    }

    @Override
    public <N> void visitSelector(Ast.Selector<N> nSelector) {
        Ast.AstVisitor.super.visitSelector(nSelector);
    }

    @Override
    public <N> void visitSegment(Ast.Segment<N> nSegment) {
        Ast.AstVisitor.super.visitSegment(nSegment);
    }

    @Override
    public <N> void visitPath(Ast.Path<N> nPath) {
        Ast.AstVisitor.super.visitPath(nPath);
        TreeAdapter<N> treeAdapter = null;
        PathEvaluator<N> nPathEvaluator = new PathEvaluator<>(treeAdapter);
        var op = BaseTypedOp.of(N.class, N.class, (value, opContext) -> nPathEvaluator.eval(value, nPath));
        nPath.setDecl(op);
    }
}
