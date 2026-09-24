/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.antlr.v4.runtime.tree.ParseTreeWalker;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.filter.record.manipulation.expr.ExprBaseVisitor;
import io.kroxylicious.filter.record.manipulation.expr.ExprParser;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpConfig;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.xform.path.Ast;
import io.kroxylicious.filter.record.manipulation.xform.path.KafkaRecordTypeSystem;
import io.kroxylicious.filter.record.manipulation.xform.path.TypeEvaluator;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.internal.Record;

import edu.umd.cs.findbugs.annotations.NonNull;

public class Check {

    private final PluginLookup pluginLookup;

    public Check(PluginLookup pluginLookup) {
        this.pluginLookup = Objects.requireNonNull(pluginLookup);
    }

    void check(RecordTransformConfig2 config) {
        Map<String, OpConfig> decls = config.where();
        ParseTreeWalker walker = new ParseTreeWalker();
        var ops = resolve(config, decls, walker);
        // typeCheck(config, decls, ops, walker);
    }

    @NonNull
    private Map<String, BaseTypedOp<?, ?>> resolve(RecordTransformConfig2 config, Map<String, OpConfig> decls, ParseTreeWalker walker) {

        var keyTree = config.key().tree().accept(new AstBuilder());
        var valueTree = config.value().tree().accept(new AstBuilder());
        var timestampTree = config.timestamp().tree().accept(new AstBuilder());
        var headersTree = config.headers().tree().accept(new AstBuilder());

        var ops = new HashMap<String, BaseTypedOp<?, ?>>();
        List<String> errors = new ArrayList<>();
        // TODO the resolver builder should really be an AstVisitor
        ResolverVisitor visitor = new ResolverVisitor(pluginLookup, decls, ops, errors);
        visitor.resolve("key", keyTree);
        visitor.resolve("value", valueTree);
        visitor.resolve("timestamp", timestampTree);
        visitor.resolve("headers", headersTree);
        if (!errors.isEmpty()) {
            throw new CheckException(String.join("\n", errors) + "\n(Hint: functions are defined using the 'where' property)");
        }
        keyTree.accept(new Typer2("key", ByteBuffer.class, ops, errors));
        valueTree.accept(new Typer2("value", ByteBuffer.class, ops, errors));
        timestampTree.accept(new Typer2("timstamp", Long.TYPE, ops, errors));
        headersTree.accept(new Typer2("headers", Header[].class, ops, errors));
        if (!errors.isEmpty()) {
            throw new CheckException(String.join("\n", errors));
        }
        return ops;
    }

    // private void typeCheck(RecordTransformConfig2 config, Map<String, OpConfig> decls, Map<String, BaseTypedOp<?, ?>> ops, ParseTreeWalker walker) {
    // List<String> errors = new ArrayList<>();
    // Typer typer = new Typer(ops, errors);
    // typer.propertyName = "key";
    // typer.requiredType = ByteBuffer.class;
    // typer.visit(config.key().tree());
    // typer.propertyName = "value";
    // typer.requiredType = ByteBuffer.class;
    // typer.visit(config.value().tree());
    // typer.propertyName = "timestamp";
    // typer.requiredType = Long.TYPE;
    // typer.visit(config.timestamp().tree());
    // typer.propertyName = "headers";
    // typer.requiredType = TypeFactory.arrayOf(Header.class);
    // typer.visit(config.headers().tree());
    // if (!errors.isEmpty()) {
    // throw new CheckException(String.join("\n", errors));
    // }
    // }

    class Typer extends ExprBaseVisitor<Type> {
        private final Map<String, BaseTypedOp<?, ?>> ops;
        private final List<String> errors;
        public String propertyName;
        public Type requiredType;
        private String expressionText;

        Typer(Map<String, BaseTypedOp<?, ?>> ops, List<String> errors) {
            this.ops = ops;
            this.errors = errors;
        }

        @Override
        public Type visitExpr(ExprParser.ExprContext ctx) {
            this.expressionText = ctx.getText();
            Type type = super.visitExpr(ctx);
            if (!GenericTypeReflector.isSuperType(requiredType, type)) {
                errors.add("Property '%s' with expression `%s`: Expression type is not assignable to %s: Expression type is %s".formatted(
                        propertyName,
                        expressionText,
                        GenericTypeReflector.getTypeName(requiredType),
                        GenericTypeReflector.getTypeName(type)));
            }
            return type;
        }

        @Override
        public Type visitBooleanLiteral(ExprParser.BooleanLiteralContext ctx) {
            return Boolean.TYPE;
        }

        @Override
        public Type visitStringLiteral(ExprParser.StringLiteralContext ctx) {
            return String.class;
        }

        @Override
        public Type visitNumberLiteral(ExprParser.NumberLiteralContext ctx) {
            return Double.TYPE;
        }

        @Override
        public Type visitIntLiteral(ExprParser.IntLiteralContext ctx) {
            return Long.TYPE;
        }

        @Override
        public Type visitLiteral(ExprParser.LiteralContext ctx) {
            if (ctx.NULL() != null) {
                return Void.class;
            }
            return super.visitLiteral(ctx);
        }

        @Override
        public Type visitFunctionExpr(ExprParser.FunctionExprContext ctx) {
            // look up the function
            String functionName = ctx.functionName().getText();
            BaseTypedOp<?, ?> op = ops.get(functionName);
            // type it
            Type argType = ctx.functionArgument().getFirst().accept(this);
            Type paramType = op.inputType();
            // check that argType is a subtype of paramType
            checkAssignable(paramType, argType, functionName);
            Type type = op.outputType();
            return type;
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

    class Typer2 implements Ast.AstVisitor {
        private final Map<String, BaseTypedOp<?, ?>> ops;
        private final List<String> errors;
        public final String propertyName;
        public final Type requiredType;
        private String expressionText;

        Typer2(String propertyName,
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
            Type type = new TypeEvaluator().type(path, Record.class, new KafkaRecordTypeSystem());
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
}
