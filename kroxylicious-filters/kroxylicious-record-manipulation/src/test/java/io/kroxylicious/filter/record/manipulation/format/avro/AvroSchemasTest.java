/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.util.Map;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.op.OpConfig;

import static org.assertj.core.api.Assertions.assertThat;

class AvroSchemasTest {

    @Test
    void applyConfigReturnsNullWhenSchemaHasNoApplyProperty() {
        // Given
        Schema schema = new Schema.Parser().parse("""
                {"type": "string"}
                """);

        // When
        var result = AvroSchemas.applyConfig(schema);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void applyConfigParsesTheApplyChainOffASchema() {
        // Given
        Schema schema = new Schema.Parser().parse("""
                {"type": "string", "apply": [{"op": "ValueString", "value": "REDACTED"}]}
                """);

        // When
        var result = AvroSchemas.applyConfig(schema);

        // Then
        assertThat(result).containsExactly(new OpConfig("ValueString", Map.of("value", "REDACTED")));
    }

    @Test
    void applyConfigReturnsNullWhenFieldHasNoApplyProperty() {
        // Given
        Schema schema = new Schema.Parser().parse("""
                {"type": "record", "name": "R", "fields": [
                    {"name": "a", "type": "string"}
                ]}
                """);

        // When
        var result = AvroSchemas.applyConfig(schema.getField("a"));

        // Then
        assertThat(result).isNull();
    }

    @Test
    void applyConfigParsesTheApplyChainOffAField() {
        // Given
        Schema schema = new Schema.Parser().parse("""
                {"type": "record", "name": "R", "fields": [
                    {"name": "a", "type": "string", "apply": [{"op": "ValueString", "value": "REDACTED"}]}
                ]}
                """);

        // When
        var result = AvroSchemas.applyConfig(schema.getField("a"));

        // Then
        assertThat(result).containsExactly(new OpConfig("ValueString", Map.of("value", "REDACTED")));
    }

}
