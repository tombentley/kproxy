/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.Assertions.assertThat;

class RecordTransformConfig2Test {

    @Test
    void parseJsonPathExpressions() {
        var config = new YAMLMapper().readValue("""
                key: $.key
                value: $.value
                timestamp: $.timestamp
                headers: $.headers
                """, RecordTransformConfig2.class);

        assertThat(config.key().tree().expr1().jsonpathQuery()).isNotNull();
        assertThat(config.key().tree().expr1().jsonpathQuery().getText()).isEqualTo("$.key");

        assertThat(config.value().tree().expr1().jsonpathQuery()).isNotNull();
        assertThat(config.value().tree().expr1().jsonpathQuery().getText()).isEqualTo("$.value");

        assertThat(config.timestamp().tree().expr1().jsonpathQuery()).isNotNull();
        assertThat(config.timestamp().tree().expr1().jsonpathQuery().getText()).isEqualTo("$.timestamp");

        assertThat(config.headers().tree().expr1().jsonpathQuery()).isNotNull();
        assertThat(config.headers().tree().expr1().jsonpathQuery().getText()).isEqualTo("$.headers");
    }

}