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
import io.leangen.geantyref.TypeFactory;

import io.kroxylicious.filter.record.manipulation.expr.ExprBaseListener;
import io.kroxylicious.filter.record.manipulation.expr.ExprBaseVisitor;
import io.kroxylicious.filter.record.manipulation.expr.ExprParser;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpConfig;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.kafka.common.header.Header;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class Check {

    private PluginLookup pluginLookup;

    public Check(PluginLookup pluginLookup) {
        this.pluginLookup = Objects.requireNonNull(pluginLookup);
    }

    void check(RecordTransformConfig2 config) {
        Map<String, OpConfig> decls = config.where();
        ParseTreeWalker walker = new ParseTreeWalker();
        var ops = resolve(config, decls, walker);
        typeCheck(config, decls, ops, walker);
    }

    @NonNull
    private Map<String, BaseTypedOp<?, ?>> resolve(RecordTransformConfig2 config, Map<String, OpConfig> decls, ParseTreeWalker walker) {
        List<String> errors = new ArrayList<>();
        var ops = new HashMap<String, BaseTypedOp<?, ?>>();
        Resolver resolver = new Resolver(decls, ops, errors);
        resolver.propertyName = "key";
        walker.walk(resolver, config.key().tree());
        resolver.propertyName = "value";
        walker.walk(resolver, config.value().tree());
        resolver.propertyName = "timestamp";
        walker.walk(resolver, config.timestamp().tree());
        resolver.propertyName = "headers";
        walker.walk(resolver, config.headers().tree());
        if (!errors.isEmpty()) {
            throw new CheckException(String.join("\n", errors) + "\n(Hint: functions are defined using the 'where' property)");
        }
        return ops;
    }

    private class Resolver extends ExprBaseListener {
        private final List<String> errors;
        private final @Nullable Map<String, OpConfig> decls;
        String propertyName;
        private String expressionText;
        private Map<String, BaseTypedOp<?, ?>> ops;

        Resolver(@Nullable Map<String, OpConfig> decls,
                 Map<String, BaseTypedOp<?, ?>> ops,
                 List<String> errors) {
            this.decls = decls;
            this.ops = ops;
            this.errors = errors;
        }

        @Override
        public void enterExpr(ExprParser.ExprContext ctx) {
            this.expressionText = ctx.getText();
        }

        @Override
        public void enterFunctionName(ExprParser.FunctionNameContext ctx) {
            var functionName = ctx.getText();
            if (decls == null || !decls.containsKey(functionName)) {
                errors.add(String.format("Property '%s' has expression `%s` which references an undefined function '%s'",
                        propertyName, expressionText, functionName));
            }
            else {
                try {
                    // TODO functions should probably be instantiated for each call site
                    // because their type attribution could vary by call site.
                    OpFactory<?, ?> f = pluginLookup.pluginInstance(OpFactory.class, decls.get(functionName).op());
                    var op = f.create(decls.get(functionName).config(), pluginLookup, null);
                    ops.put(functionName, op);
                }
                catch (Exception e) {
                    errors.add(String.format("Could not instantiate function '%s' referred to in expression for property '%s': %s", functionName, propertyName,
                            e.getMessage()));
                }
            }
        }

        @Override
        public void enterStringLiteral(ExprParser.StringLiteralContext ctx) {
            if (ctx.STRING() != null) {
                // Our grammar allows stuff that the RFC ABNF does not, so we need to reject that
                String str = ctx.STRING().getText();
                // TODO this should probably be a tree rewriter, so that later passes over the tree can just use the
                // ctx.STRING().getText() and trust the value
                // (i.e. this is not _just_ about checking, but also evaluating the escapes
                unescape(str);
            }
        }
    }

    static String unescape(String raw) {
        if (raw == null || raw.length() < 2) {
            return "";
        }
        // Remove enclosing quotes (' or ")
        String content = raw.substring(1, raw.length() - 1);
        StringBuilder sb = new StringBuilder(content.length());

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\' && i + 1 < content.length()) {
                char next = content.charAt(++i);
                switch (next) {
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '/' -> sb.append('/');
                    case '\\' -> sb.append('\\');
                    case '\'' -> sb.append('\'');
                    case '"' -> sb.append('"');
                    case 'u' -> {
                        // Decode \\uXXXX and handle UTF-16 surrogate pairs
                        String hex = content.substring(i + 1, i + 5);
                        int codePoint = Integer.parseInt(hex, 16);
                        if (!Character.isValidCodePoint(codePoint)) {
                            throw new IllegalArgumentException("Invalid unicode code point: \\u" + hex);
                        }
                        sb.append((char) codePoint);
                        i += 4;
                    }
                    default -> throw new IllegalArgumentException("Invalid escape sequence: \\" + next);
                }
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void typeCheck(RecordTransformConfig2 config, Map<String, OpConfig> decls, Map<String, BaseTypedOp<?, ?>> ops, ParseTreeWalker walker) {
        List<String> errors = new ArrayList<>();
        Typer typer = new Typer(ops, errors);
        typer.propertyName = "key";
        typer.requiredType = ByteBuffer.class;
        typer.visit(config.key().tree());
        typer.propertyName = "value";
        typer.requiredType = ByteBuffer.class;
        typer.visit(config.value().tree());
        typer.propertyName = "timestamp";
        typer.requiredType = Long.TYPE;
        typer.visit(config.timestamp().tree());
        typer.propertyName = "headers";
        typer.requiredType = TypeFactory.arrayOf(Header.class);
        typer.visit(config.headers().tree());
        if (!errors.isEmpty()) {
            throw new CheckException(String.join("\n", errors));
        }
    }

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
            checkAssignable(requiredType, type, "bob");
            return type;
        }

        @Override
        public Type visitBooleanLiteral(ExprParser.BooleanLiteralContext ctx) {
            return Boolean.class;
        }

        @Override
        public Type visitStringLiteral(ExprParser.StringLiteralContext ctx) {
            return String.class;
        }

        @Override
        public Type visitNumberLiteral(ExprParser.NumberLiteralContext ctx) {
            return Double.class;
        }

        @Override
        public Type visitIntLiteral(ExprParser.IntLiteralContext ctx) {
            return Long.class;
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

        private void checkAssignable(Type paramType, Type argType, String functionName) {
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

    // private class Typer2 extends ExprBaseListener {
    // private final List<String> errors;
    // private final @Nullable Map<String, OpConfig> decls;
    // String propertyName;
    // final Map<String, BaseTypedOp<?, ?>> ops = new HashMap<>();
    // org.antlr.v4.runtime.tree.ParseTreeProperty<Type> types;
    //
    // Typer2(@Nullable Map<String, OpConfig> decls, List<String> errors) {
    // this.decls = decls;
    // this.errors = errors;
    // }
    //
    // @Override
    // public void exitLiteral(ExprParser.LiteralContext ctx) {
    // if (ctx.FALSE() != null || ctx.TRUE() != null) {
    // types.put(ctx, Boolean.class);
    // }
    // else if (ctx.STRING() != null) {
    // types.put(ctx, String.class);
    // }
    // else if (ctx.NUMBER() != null) {
    // String text = ctx.NUMBER().getSymbol().getText();
    // if (text.contains(".")) {
    // types.put(ctx, Double.class);
    // } else {
    // types.put(ctx, Long.class);
    // }
    // }
    // else if (ctx.NULL() != null) {
    // types.put(ctx, Void.class);
    // }
    // super.exitLiteral(ctx);
    // }
    //
    // @Override
    // public void exitComparable(ExprParser.ComparableContext ctx) {
    // if (ctx.literal() != null) {
    // types.put(ctx, types.get(ctx.literal()));
    // }
    // else if (ctx.singularQuery() != null) {
    // types.put(ctx, types.get(ctx.singularQuery()));
    // }
    // else if (ctx.functionExpr() != null) {
    // types.put(ctx, types.get(ctx.functionExpr()));
    // }
    // super.exitComparable(ctx);
    // }
    // }
}
