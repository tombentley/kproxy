/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.exc.ValueInstantiationException;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordTransformConfig2Test {

    @Test
    void deserializeFailedOnInvalidJsonPathExpressions() {
        // Given
        String content = """
                key: $.key
                value: $.value
                timestamp: $.timestamp
                headers: foo
                """;
        YAMLMapper yamlMapper = new YAMLMapper();

        // When/Then
        assertThatThrownBy(() -> yamlMapper.readValue(content, RecordTransformConfig2.class))
                .isInstanceOf(ValueInstantiationException.class)
                .hasMessage("""
                        Cannot construct instance of `io.kroxylicious.filter.record.manipulation.kafka.Expr`, problem: Found 1 syntax errors
                        1:3: no viable alternative at input 'foo'.
                        
                         at [Source: (StringReader); line: 4, column: 10] (through reference chain: io.kroxylicious.filter.record.manipulation.kafka.RecordTransformConfig2["headers"])""");

    }

    @Test
    void deserializeJsonPathExpressions() {
        // Given
        String content = """
                key: $.key
                value: $.value
                timestamp: $.timestamp
                headers: foo(1, $.headers)
                """;

        // When
        var config = new YAMLMapper().readValue(content, RecordTransformConfig2.class);

        // Then
        assertThat(config.key().tree().expr1().jsonpathQuery()).isNotNull();
        assertThat(config.key().tree().expr1().getText()).isEqualTo("$.key");

        assertThat(config.value().tree().expr1().jsonpathQuery()).isNotNull();
        assertThat(config.value().tree().expr1().getText()).isEqualTo("$.value");

        assertThat(config.timestamp().tree().expr1().jsonpathQuery()).isNotNull();
        assertThat(config.timestamp().tree().expr1().getText()).isEqualTo("$.timestamp");

        assertThat(config.headers().tree().expr1().logicalExpr()).isNotNull();
        assertThat(config.headers().tree().expr1().getText()).isEqualTo("foo(1, $.headers)");
    }

    @Test
    void serializeJsonPathExpressions() {
        // Given
        YAMLMapper yamlMapper = new YAMLMapper();
        var config = yamlMapper.readValue("""
                key: $.key
                value: $.value
                timestamp: $.timestamp
                headers: foo(1, $.headers)
                """, RecordTransformConfig2.class);

        // When
        var str = yamlMapper.writeValueAsString(config);

        // Then
        assertThat(str).isEqualTo("""
                ---
                key: "$.key"
                value: "$.value"
                timestamp: "$.timestamp"
                headers: "foo(1, $.headers)"
                """);
    }

}
