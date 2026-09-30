/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.xform.path;

import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.transform.neo.Expr;
import io.kroxylicious.transform.neo.ExprLexer;
import io.kroxylicious.transform.neo.ExprParser;
import io.kroxylicious.transform.neo.RecordTransformConfig2;
import io.kroxylicious.transform.neo.op.OpConfig;
import io.kroxylicious.transform.service.PluginLookup;

public class Check {

    private final PluginLookup pluginLookup;

    public Check(PluginLookup pluginLookup) {
        this.pluginLookup = Objects.requireNonNull(pluginLookup);
    }

    public static ExprParser.ExprContext parseExpr(Expr expr) {
        var stream = CharStreams.fromString(expr.node());
        var lexer = new ExprLexer(stream);
        // lexer.removeErrorListeners();

        ParseErrorListener listener = new ParseErrorListener(100);
        lexer.addErrorListener(listener);
        var tokenStream = new CommonTokenStream(lexer);
        var parser = new ExprParser(tokenStream);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        ExprParser.ExprContext tree = parser.expr();
        listener.maybeThrow();

        return tree;
    }

    interface ErrorCollector {

        int numErrors();

        int maxErrorsToReport();

        List<String> errorMessages();

        default void maybeThrow(String message) {
            if (this.numErrors() > 0) {
                StringBuilder msg = new StringBuilder();
                msg.append(message.formatted(this.numErrors()));
                if (this.numErrors() > this.maxErrorsToReport()) {
                    msg.append(" (showing first %d)".formatted(this.maxErrorsToReport()));
                }
                msg.append(System.lineSeparator());
                this.errorMessages().forEach(e -> msg.append(e).append(System.lineSeparator()));
                throw new InvalidExpressionException(msg.toString(), this.errorMessages());
            }
        }
    }

    static class ParseErrorListener extends BaseErrorListener implements ErrorCollector {
        private final List<String> errorMessages;
        private final int maxErrorsToReport;
        private int numErrors = 0;

        ParseErrorListener(int maxErrorsToReport) {
            this.maxErrorsToReport = maxErrorsToReport;
            this.errorMessages = new ArrayList<>(maxErrorsToReport);
        }

        @Override
        public List<String> errorMessages() {
            return errorMessages;
        }

        @Override
        public int numErrors() {
            return numErrors;
        }

        @Override
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

        public void maybeThrow() {
            maybeThrow("Found %d syntax errors");
        }
    }

    void check(RecordTransformConfig2 config) {
        check(config, false);
    }

    MethodHandle check(RecordTransformConfig2 config, boolean i) {
        Map<String, OpConfig> decls = config.where();

        // Build ASTs from the parse trees
        var keyTree = parseExpr(config.key()).accept(new AstBuilder());
        var valueTree = parseExpr(config.value()).accept(new AstBuilder());
        var timestampTree = parseExpr(config.timestamp()).accept(new AstBuilder());
        var headersTree = parseExpr(config.headers()).accept(new AstBuilder());
        // Resolve the function calls in the expressions to their declarations via the `where` clause
        resolve(decls, keyTree, valueTree, timestampTree, headersTree);
        // Type check the expressions
        typeCheck(keyTree, valueTree, timestampTree, headersTree);

        if (i) {
            HandleVisitor visitor = new HandleVisitor();
            valueTree.accept(visitor);
            return visitor.handle;
        }
        return null;
    }

    void resolve(Map<String, OpConfig> decls,
                 Ast keyTree,
                 Ast valueTree,
                 Ast timestampTree,
                 Ast headersTree) {
        List<String> errors = new ArrayList<>();
        ResolverVisitor visitor = new ResolverVisitor(pluginLookup, decls, errors);
        visitor.resolve("key", keyTree);
        visitor.resolve("value", valueTree);
        visitor.resolve("timestamp", timestampTree);
        visitor.resolve("headers", headersTree);
        if (!errors.isEmpty()) {
            throw new CheckException(String.join("\n", errors) + "\n(Hint: functions are defined using the 'where' property)");
        }
    }

    private void typeCheck(Ast keyTree,
                           Ast valueTree,
                           Ast timestampTree,
                           Ast headersTree) {
        List<String> errors = new ArrayList<>();
        keyTree.accept(new TypeCheckingVisitor("key", ByteBuffer.class, errors));
        valueTree.accept(new TypeCheckingVisitor("value", ByteBuffer.class, errors));
        timestampTree.accept(new TypeCheckingVisitor("timestamp", Long.TYPE, errors));
        headersTree.accept(new TypeCheckingVisitor("headers", Header[].class, errors));
        if (!errors.isEmpty()) {
            throw new CheckException(String.join("\n", errors));
        }
    }

}
