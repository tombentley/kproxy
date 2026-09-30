/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.format.jackson3;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import io.kroxylicious.transform.neo.TypeBaseVisitor;
import io.kroxylicious.transform.neo.TypeLexer;
import io.kroxylicious.transform.neo.TypeParser;
import io.kroxylicious.transform.neo.type.Union;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

public record TypeExpression(JavaType jacksonType, Type opType) {

    private TypeExpression(TypeParser.TypeContext tree, TypeFactory typeFactory) {
        this(new JacksonJavaTypeVisitor(typeFactory).visit(tree),
                new JvmTypeVisitor().visit(tree));
    }

    static TypeExpression parse(String typeExpression, TypeFactory typeFactory) {
        var stream = CharStreams.fromString(typeExpression);
        var lexer = new TypeLexer(stream);
        lexer.removeErrorListeners();
        var listener = new ParseErrorListener(100);
        lexer.addErrorListener(listener);
        var tokenStream = new CommonTokenStream(lexer);
        var parser = new TypeParser(tokenStream);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        TypeParser.TypeContext tree = parser.type();
        listener.maybeThrow("");

        return new TypeExpression(tree, typeFactory);
    }

    static class ParseErrorListener extends BaseErrorListener {
        private final List<String> errorMessages;
        private final int maxErrorsToReport;
        private int numErrors = 0;

        ParseErrorListener(int maxErrorsToReport) {
            this.maxErrorsToReport = maxErrorsToReport;
            this.errorMessages = new ArrayList<>(maxErrorsToReport);
        }

        public List<String> errorMessages() {
            return errorMessages;
        }

        public int numErrors() {
            return numErrors;
        }

        public int maxErrorsToReport() {
            return maxErrorsToReport;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            numErrors++;
            if (errorMessages.size() < maxErrorsToReport) {
                errorMessages.add("%d:%d: %s".formatted(line, charPositionInLine, msg.endsWith(".") ? msg : msg + "."));
            }
        }

        public void maybeThrow(String message) {
            if (this.numErrors() > 0) {
                StringBuilder msg = new StringBuilder();
                msg.append(message.formatted(this.numErrors()));
                if (this.numErrors() > this.maxErrorsToReport()) {
                    msg.append(" (showing first %d)".formatted(this.maxErrorsToReport()));
                }
                msg.append(System.lineSeparator());
                this.errorMessages().forEach(e -> msg.append(e).append(System.lineSeparator()));
                throw new InvalidTypeExpressionException(msg.toString(), this.errorMessages());
            }
        }
    }

    /**
     * A visitor for the Type.g4 parse tree which converts type expressions into Jackson's {@link JavaType}
     * This is needed because although {@link TypeFactory#constructFromCanonical(String)} exists
     * we also need a way to construct our own representation for use with BaseTypedOp.
     * Our representation uses the JVM's own Type subclasses (not JavaType)
     */
    private static class JacksonJavaTypeVisitor extends TypeBaseVisitor<JavaType> {
        private final TypeFactory typeFactory;

        JacksonJavaTypeVisitor(TypeFactory typeFactory) {
            this.typeFactory = typeFactory;
        }

        @Override
        public JavaType visitTypeName(TypeParser.TypeNameContext ctx) {
            Class<?> c = null;
            try {
                String text = ctx.getText();
                c = typeFactory.findClass(text);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            return typeFactory.constructType(c);
        }

        @Override
        public JavaType visitBaseType(TypeParser.BaseTypeContext ctx) {
            JavaType accept = ctx.typeName().accept(this);
            JavaType javaType;
            if (ctx.typeArgs() != null) {
                Class<?> rawClass = accept.getRawClass();
                var typeArgs = ctx.typeArgs().typeArg().stream().map(x -> x.accept(this)).toArray(JavaType[]::new);
                javaType = typeFactory.constructParametricType(rawClass, typeArgs);
            }
            else {
                javaType = accept;
            }
            if (ctx.LSQ() != null) {
                for (var i = 0; i < ctx.LSQ().size(); i++) {
                    javaType = typeFactory.constructArrayType(javaType);
                }
            }
            return javaType;
        }
    }

    private static class JvmTypeVisitor extends TypeBaseVisitor<Type> {

        JvmTypeVisitor() {
        }

        @Override
        public Type visitTypeName(TypeParser.TypeNameContext ctx) {
            try {
                String name = ctx.getText();
                return Class.forName(name);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Type visitBaseType(TypeParser.BaseTypeContext ctx) {
            Type accept = ctx.typeName().accept(this);
            Type javaType;
            if (ctx.typeArgs() != null) {
                Class<?> rawClass = (Class<?>) accept;
                var typeArgs = ctx.typeArgs().typeArg().stream().map(x -> x.accept(this)).toArray(Type[]::new);
                javaType = io.leangen.geantyref.TypeFactory.parameterizedClass(rawClass, typeArgs);
            }
            else {
                javaType = accept;
            }
            if (ctx.LSQ() != null) {
                for (var i = 0; i < ctx.LSQ().size(); i++) {
                    javaType = io.leangen.geantyref.TypeFactory.arrayOf(javaType);
                }
            }
            return javaType;
        }

        @Override
        public Type visitUnionType(TypeParser.UnionTypeContext ctx) {
            return Union.of(ctx.baseType().stream().map(x -> x.accept(this)).toList());
        }

        @Override
        public Type visitIntersectionType(TypeParser.IntersectionTypeContext ctx) {
            return super.visitIntersectionType(ctx);
        }
    }
}
