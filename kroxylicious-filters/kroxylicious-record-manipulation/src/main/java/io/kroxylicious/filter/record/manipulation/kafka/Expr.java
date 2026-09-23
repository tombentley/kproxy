/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import java.io.IOException;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.kroxylicious.filter.record.manipulation.expr.ExprBaseListener;
import io.kroxylicious.filter.record.manipulation.expr.ExprLexer;
import io.kroxylicious.filter.record.manipulation.expr.ExprParser;

@JsonDeserialize(using = Expr.Deser.class)
@JsonSerialize(using = Expr.Ser.class)
public class Expr {

    static class Deser extends JsonDeserializer<Expr> {

        @Override
        public Expr deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            String text = p.getText();
            return fromString(text);
        }
    }

    static class Ser extends JsonSerializer<Expr> {

        @Override
        public void serialize(Expr expr, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(expr.text);
        }
    }

    private final ExprParser.ExprContext tree;
    private final String text;

    @JsonCreator
    public static Expr fromString(String text) {
        var stream = CharStreams.fromString(text);
        var lexer = new ExprLexer(stream);
        var tokenStream = new CommonTokenStream(lexer);
        var parser = new ExprParser(tokenStream);
        parser.removeErrorListeners();
        ExprParser.ExprContext tree = parser.expr();

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new ExprBaseListener() {
        }, tree);

        return new Expr(text, tree);
    }

    @JsonValue
    public String text() {
        return text;
    }

    private Expr(String text, ExprParser.ExprContext tree) {
        this.text = text;
        this.tree = tree;
    }

    public ExprParser.ExprContext tree() {
        return tree;
    }
}
