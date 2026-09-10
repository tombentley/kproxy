/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonDeserialize(using = Expr.Deser.class)
@JsonSerialize(using = Expr.Ser.class)
public class Expr {
    static class Deser extends JsonDeserializer<Expr> {

        @Override
        public Expr deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            String text = p.getText();
            return new Expr(text);
        }
    }

    static class Ser extends JsonSerializer<Expr> {

        @Override
        public void serialize(Expr expr, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(expr.value);
        }
    }

    private final String value;

    public Expr(String value) {
        this.value = value;
    }

}
