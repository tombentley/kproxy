/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.ServiceLoaderPluginLookup;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvroFunctionTest {

    private static final OpContext OP_CONTEXT = new OpContext(new Random(), new byte[0]);
    private static final PluginLookup LOOKUP = new ServiceLoaderPluginLookup();

    private static Schema parse(String schemaJson) {
        return new Schema.Parser().parse(schemaJson);
    }

    @Test
    void buildMaskAppliesStringOpToAnnotatedField() {
        // Given
        Schema schema = parse("""
                {"type": "record", "name": "R", "fields": [
                    {"name": "firstName", "type": "string", "apply": [{"op": "ValueString", "value": "REDACTED"}]},
                    {"name": "surname", "type": "string"}
                ]}
                """);
        GenericRecord input = new GenericData.Record(schema);
        input.put("firstName", "Harry");
        input.put("surname", "Potter");

        // When
        GenericRecord result = (GenericRecord) AvroFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        assertThat(result.get("firstName")).isEqualTo("REDACTED");
        assertThat(result.get("surname")).isEqualTo("Potter");
    }

    @Test
    void buildMaskAppliesIntOpToAnnotatedField() {
        // Given
        Schema schema = parse("""
                {"type": "record", "name": "R", "fields": [
                    {"name": "ageYears", "type": "int", "apply": [{"op": "ValueInt", "value": 99}]}
                ]}
                """);
        GenericRecord input = new GenericData.Record(schema);
        input.put("ageYears", 17);

        // When
        GenericRecord result = (GenericRecord) AvroFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        assertThat(result.get("ageYears")).isEqualTo(99);
    }

    @Test
    void buildMaskRecursesIntoNestedRecordFields() {
        // Given
        Schema schema = parse("""
                {"type": "record", "name": "R", "fields": [
                    {"name": "address", "type": {"type": "record", "name": "Address", "fields": [
                        {"name": "city", "type": "string", "apply": [{"op": "ValueString", "value": "REDACTED"}]}
                    ]}}
                ]}
                """);
        GenericRecord address = new GenericData.Record(schema.getField("address").schema());
        address.put("city", "Hogsmeade");
        GenericRecord input = new GenericData.Record(schema);
        input.put("address", address);

        // When
        GenericRecord result = (GenericRecord) AvroFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        assertThat(((GenericRecord) result.get("address")).get("city")).isEqualTo("REDACTED");
    }

    @Test
    void buildMaskAppliesOpToEachArrayElement() {
        // Given
        Schema schema = parse("""
                {"type": "record", "name": "R", "fields": [
                    {"name": "aliases", "type": {"type": "array", "items": {"type": "string", "apply": [{"op": "ValueString", "value": "REDACTED"}]}}}
                ]}
                """);
        GenericRecord input = new GenericData.Record(schema);
        input.put("aliases", List.of("Vernon Dudley", "Barny Weasley"));

        // When
        GenericRecord result = (GenericRecord) AvroFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        assertThat((List<Object>) result.get("aliases")).containsExactly("REDACTED", "REDACTED");
    }

    @Test
    void buildMaskAppliesBytesOpToAnnotatedField() {
        // Given
        Schema schema = parse("""
                {"type": "record", "name": "R", "fields": [
                    {"name": "photo", "type": "bytes", "apply": [{"op": "ValueBytes", "value": [9, 9, 9]}]}
                ]}
                """);
        GenericRecord input = new GenericData.Record(schema);
        input.put("photo", ByteBuffer.wrap(new byte[]{ 1, 2, 3 }));

        // When
        GenericRecord result = (GenericRecord) AvroFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        assertThat((ByteBuffer) result.get("photo")).isEqualTo(ByteBuffer.wrap(new byte[]{ 9, 9, 9 }));
    }

    @Test
    void buildMaskAppliesEnumOpAndAcceptsAllowedSymbol() {
        // Given
        Schema schema = parse("""
                {"type": "record", "name": "R", "fields": [
                    {"name": "color", "type": {"type": "enum", "name": "Color", "symbols": ["RED", "GREEN", "BLUE"]},
                     "apply": [{"op": "ValueString", "value": "GREEN"}]}
                ]}
                """);
        Schema colorSchema = schema.getField("color").schema();
        GenericRecord input = new GenericData.Record(schema);
        input.put("color", new GenericData.EnumSymbol(colorSchema, "RED"));

        // When
        GenericRecord result = (GenericRecord) AvroFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        assertThat(result.get("color")).isEqualTo(new GenericData.EnumSymbol(colorSchema, "GREEN"));
    }

    @Test
    void buildMaskThrowsWhenEnumOpProducesDisallowedSymbol() {
        // Given
        Schema schema = parse("""
                {"type": "record", "name": "R", "fields": [
                    {"name": "color", "type": {"type": "enum", "name": "Color", "symbols": ["RED", "GREEN", "BLUE"]},
                     "apply": [{"op": "ValueString", "value": "PURPLE"}]}
                ]}
                """);
        Schema colorSchema = schema.getField("color").schema();
        GenericRecord input = new GenericData.Record(schema);
        input.put("color", new GenericData.EnumSymbol(colorSchema, "RED"));
        var mask = AvroFunction.buildMask(schema, LOOKUP);

        // When/Then
        assertThatThrownBy(() -> mask.apply(input, OP_CONTEXT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PURPLE");
    }

    @Test
    void buildMaskAppliesFixedOpAndAcceptsCorrectSize() {
        // Given
        Schema schema = parse("""
                {"type": "record", "name": "R", "fields": [
                    {"name": "fingerprint", "type": {"type": "fixed", "name": "Fingerprint", "size": 4},
                     "apply": [{"op": "ValueBytes", "value": [9, 9, 9, 9]}]}
                ]}
                """);
        Schema fixedSchema = schema.getField("fingerprint").schema();
        GenericRecord input = new GenericData.Record(schema);
        input.put("fingerprint", new GenericData.Fixed(fixedSchema, new byte[]{ 1, 2, 3, 4 }));

        // When
        GenericRecord result = (GenericRecord) AvroFunction.buildMask(schema, LOOKUP).apply(input, OP_CONTEXT);

        // Then
        assertThat(result.get("fingerprint")).isEqualTo(new GenericData.Fixed(fixedSchema, new byte[]{ 9, 9, 9, 9 }));
    }

    @Test
    void buildMaskThrowsWhenFixedOpProducesWrongSize() {
        // Given
        Schema schema = parse("""
                {"type": "record", "name": "R", "fields": [
                    {"name": "fingerprint", "type": {"type": "fixed", "name": "Fingerprint", "size": 4},
                     "apply": [{"op": "ValueBytes", "value": [9, 9, 9]}]}
                ]}
                """);
        Schema fixedSchema = schema.getField("fingerprint").schema();
        GenericRecord input = new GenericData.Record(schema);
        input.put("fingerprint", new GenericData.Fixed(fixedSchema, new byte[]{ 1, 2, 3, 4 }));
        var mask = AvroFunction.buildMask(schema, LOOKUP);

        // When/Then
        assertThatThrownBy(() -> mask.apply(input, OP_CONTEXT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fingerprint");
    }

    @Test
    void buildMaskAppliesOpDirectlyToABareScalarRootSchema() {
        // Given
        Schema schema = parse("""
                {"type": "string", "apply": [{"op": "ValueString", "value": "REDACTED"}]}
                """);

        // When
        Object result = AvroFunction.buildMask(schema, LOOKUP).apply("Harry", OP_CONTEXT);

        // Then
        assertThat(result).isEqualTo("REDACTED");
    }

    @Test
    void buildMaskAppliesOpToEachElementOfAnArrayRootSchema() {
        // Given
        Schema schema = parse("""
                {"type": "array", "items": {"type": "string", "apply": [{"op": "ValueString", "value": "REDACTED"}]}}
                """);

        // When
        Object result = AvroFunction.buildMask(schema, LOOKUP).apply(List.of("Vernon Dudley", "Barny Weasley"), OP_CONTEXT);

        // Then
        assertThat((List<Object>) result).containsExactly("REDACTED", "REDACTED");
    }

}
