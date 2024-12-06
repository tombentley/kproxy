/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * A SchemaObject or a boolean (not both).
 *
 */
@JsonSerialize(using = SchemaValue.Serializer.class)
@JsonDeserialize(using = SchemaValue.Deserializer.class)
public class SchemaValue {

    // Invariant: exactly one of the fields is not null.
    @Nullable Boolean booleanValue;
    @Nullable SchemaObject schemaObject;

    public SchemaValue(
            boolean booleanValue
    ) {
        this.booleanValue = booleanValue;
        this.schemaObject = null;
    }

    public SchemaValue(@NonNull SchemaObject schemaObject) {
        this.booleanValue = null;
        this.schemaObject = Objects.requireNonNull(schemaObject);
    }

    /**
     * @return The boolean value, or null if this value is actually a schema
     */
    public @Nullable Boolean getBooleanValue() {
        return booleanValue;
    }

    /**
     * Sets the boolean value (clearing any existing schema value).
     * @param booleanValue
     */
    public void setBooleanValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
        this.schemaObject = null;
    }

    /**
     * @return The schema, or null if this value is actually a boolean
     */
    @Nullable
    public SchemaObject getSchemaObject() {
        return schemaObject;
    }

    /**
     * Sets the schema value, (clearing any existing boolean value)
     * @param schemaObject
     */
    public void setSchemaObject(@NonNull SchemaObject schemaObject) {
        this.schemaObject = Objects.requireNonNull(schemaObject);
        this.booleanValue = null;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SchemaValue that)) {
            return false;
        }
        return Objects.equals(booleanValue, that.booleanValue) && Objects.equals(schemaObject, that.schemaObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(booleanValue, schemaObject);
    }

    @Override
    public String toString() {
        return String.valueOf(schemaObject != null ? schemaObject.getSchema() : booleanValue);
    }

    public static class Serializer extends JsonSerializer<SchemaValue> {
        @Override
        public void serialize(SchemaValue jsonSchemaPropsOrBool,
                              JsonGenerator jsonGenerator,
                              SerializerProvider serializerProvider) throws IOException {
            if (jsonSchemaPropsOrBool.getSchemaObject() != null) {
                jsonGenerator.writeObject(jsonSchemaPropsOrBool.getSchemaObject());
            }
            else {
                jsonGenerator.writeBoolean(jsonSchemaPropsOrBool.getBooleanValue());
            }
        }
    }

    public static class Deserializer extends JsonDeserializer<SchemaValue> {

        @Override
        public SchemaValue deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {
            if (jsonParser.isExpectedStartObjectToken()) {
                return new SchemaValue(
                        jsonParser.readValueAs(SchemaObject.class));
            }
            else {
                return new SchemaValue(jsonParser.getBooleanValue());
            }
        }
    }
}
