/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.kafka.common.record.internal.Record;

class TypeCheckingVisitor implements Ast.AstVisitor {
    private final Map<String, BaseTypedOp<?, ?>> ops;
    private final List<String> errors;
    public final String propertyName;
    public final Type requiredType;
    private String expressionText;

    TypeCheckingVisitor(String propertyName,
                        Type requiredType,
                        Map<String, BaseTypedOp<?, ?>> ops, List<String> errors) {
        this.propertyName = propertyName;
        this.requiredType = requiredType;
        this.ops = ops;
        this.errors = errors;
    }

    @Override
    public void visitExpr(Ast.Expr expr) {
        this.expressionText = expr.tree().getText();
        expr.expr().accept(this);
        Type type = expr.expr().type();
        if (!GenericTypeReflector.isSuperType(requiredType, type)) {
            errors.add("Property '%s' with expression `%s`: Expression type is not assignable to %s: Expression type is %s".formatted(
                    propertyName,
                    expressionText,
                    GenericTypeReflector.getTypeName(requiredType),
                    GenericTypeReflector.getTypeName(type)));
        }
    }

    @Override
    public void visitBool(Ast.Bool ctx) {
        ctx.setType(Boolean.TYPE);
    }

    @Override
    public void visitStr(Ast.Str ctx) {
        ctx.setType(String.class);
    }

    @Override
    public void visitNum(Ast.Num ctx) {
        ctx.setType(Double.TYPE);
    }

    @Override
    public void visitInt(Ast.Int ctx) {
        ctx.setType(Long.TYPE);
    }

    @Override
    public void visitNull(Ast.Null ctx) {
        ctx.setType(Void.class);
    }

    @Override
    public void visitFunctionExpr(Ast.FnCall ctx) {
        // look up the function
        String functionName = ctx.fnName();
        BaseTypedOp<?, ?> op = ops.get(functionName);
        // type it
        ctx.args().forEach(arg -> arg.accept(this));
        Type argType = ctx.args().getFirst().type();
        Type paramType = op.inputType();
        // check that argType is a subtype of paramType
        checkAssignable(paramType, argType, functionName);
        Type type = op.outputType();
        ctx.setType(type);
    }

    @Override
    public void visitBinaryOp(Ast.BinaryOp op) {
        op.left().accept(this);
        op.right().accept(this);
        op.setType(Boolean.TYPE);
    }

    @Override
    public void visitDisjunction(Ast.Disjunction disjunction) {
        disjunction.disjunctees().forEach(d -> d.accept(this));
        disjunction.setType(disjunction.disjunctees().getLast().type());
    }

    @Override
    public void visitConjunction(Ast.Conjunction conjunction) {
        conjunction.conjunctees().forEach(d -> d.accept(this));
        conjunction.setType(conjunction.conjunctees().getLast().type());
    }

    @Override
    public void visitNot(Ast.Not not) {
        Ast.AstVisitor.super.visitNot(not);
    }

    @Override
    public <N> void visitPath(Ast.Path<N> path) {
        Ast.AstVisitor.super.visitPath(path);
        Type type = new TypeEvaluator<N, Type>().type(path, Record.class, new KafkaRecordTypeSystem());
        path.setType(type);
    }

    private void checkAssignable(Type paramType,
                                 Type argType,
                                 String functionName) {
        if (!GenericTypeReflector.isSuperType(paramType, argType)) {
            addTypeError(functionName, paramType, argType);
        }
    }

    private void addTypeError(String functionName, Type paramType, Type argType) {
        errors.add("Property '%s' with expression `%s`: Function %s should take a %s, but is being called with a value of type %s".formatted(
                propertyName,
                expressionText,
                functionName,
                GenericTypeReflector.getTypeName(paramType),
                GenericTypeReflector.getTypeName(argType)));
    }
}
