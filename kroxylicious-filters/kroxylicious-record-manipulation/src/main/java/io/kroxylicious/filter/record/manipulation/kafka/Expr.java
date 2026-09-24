/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.kroxylicious.filter.record.manipulation.expr.ExprLexer;
import io.kroxylicious.filter.record.manipulation.expr.ExprParser;

import tools.jackson.databind.JsonNode;

/**
 * Data mapping type representing an expression in the expression language.
 * The expression is parsed during Jackson deserialization.
 */
public class Expr {

    @JsonCreator
    public static Expr fromString(JsonNode text) {
        String string = text.asString();
        var stream = CharStreams.fromString(string);
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

        return new Expr(tree);
    }

    private final ExprParser.ExprContext tree;

    @JsonValue
    public String text() {
        return tree.getText();
    }

    private Expr(ExprParser.ExprContext tree) {
        this.tree = tree;
    }

    public ExprParser.ExprContext tree() {
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
}
