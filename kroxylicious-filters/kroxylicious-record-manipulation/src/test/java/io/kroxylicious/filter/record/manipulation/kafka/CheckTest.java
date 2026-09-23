/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;

import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckTest {

    private Check check = new Check(new PluginLookup() {
        @Override
        public <P> P pluginInstance(Class<P> c, String n) {
            return new ServiceBasedPluginFactoryRegistry().pluginFactory(c).pluginInstance(n);
        }
    });

    @Test
    void detectsUndefinedFunction() {
        // Given
        String content = """
                key: $.key
                value: read_json($.value)
                timestamp: $.timestamp
                headers: $.headers
                """;
        var config = new YAMLMapper().readValue(content, RecordTransformConfig2.class);

        // When/Then
        assertThatThrownBy(() -> check.check(config)).isExactlyInstanceOf(CheckException.class)
                .hasMessage("""
                        Property 'value' has expression `read_json($.value)` which references an undefined function 'read_json'
                        (Hint: functions are defined using the 'where' property)""");
    }

    @Test
    void detectsDefinedFunctionWithMissingOp() {
        // Given
        String content = """
                key: $.key
                value: read_json($.value)
                timestamp: $.timestamp
                headers: $.headers
                where:
                  read_json:
                    op: io.kroxylicious.filter.record.manipulation.format.jackson100.DeserializeJson
                    format: json
                """;
        var config = new YAMLMapper().readValue(content, RecordTransformConfig2.class);

        // When/Then
        assertThatThrownBy(() -> check.check(config)).isExactlyInstanceOf(CheckException.class)
                .message().contains(
                        "Could not instantiate function 'read_json' referred to in expression for property 'value': "
                        + "Unknown io.kroxylicious.filter.record.manipulation.op.OpFactory plugin instance for name "
                        + "'io.kroxylicious.filter.record.manipulation.format.jackson100.DeserializeJson'. "
                        + "Known plugin instances are [",
                        "Plugins must be loadable by java.util.ServiceLoader and annotated with @Plugin.");
    }

    @Test
    void detectsDefinedFunctionWithBadConfigProperties() {
        // Given
        String content = """
                key: $.key
                value: read_json($.value)
                timestamp: $.timestamp
                headers: $.headers
                where:
                  read_json:
                    op: io.kroxylicious.filter.record.manipulation.format.jackson3.DeserializeJson
                    fruit: apples
                """;
        var config = new YAMLMapper().readValue(content, RecordTransformConfig2.class);

        // When/Then
        assertThatThrownBy(() -> check.check(config)).isExactlyInstanceOf(CheckException.class)
                .message().contains(
                        "Could not instantiate function 'read_json' referred to in expression for property 'value': "
                                + "Unrecognized property \"fruit\" (class io.kroxylicious.filter.record.manipulation.format.jackson3.DeserializeJson$JsonReaderConfig), "
                                + "not marked as ignorable",
                        "known properties",
                        "\"type\"",
                        "\"readFeatures\"",
                        "\"deserializationFeatures\"");
    }

    @Test
    void detectsIncompatibleArgumentType() {
        // Given
        String content = """
                key: write_json("")
                value: write_json(1)
                timestamp: 12
                headers: write_json(true)
                where:
                  write_json:
                    op: io.kroxylicious.filter.record.manipulation.format.jackson3.SerializeJson
                    format: json
                """;
        var config = new YAMLMapper().readValue(content, RecordTransformConfig2.class);

        // When/Then
        assertThatThrownBy(() -> check.check(config))
                .isExactlyInstanceOf(CheckException.class)
                .message().contains("Property 'value' with expression `read_json(1)`: Function read_json should take a java.nio.ByteBuffer, but is being called with a value of type java.lang.Long");
    }

    @Test
    void acceptsDefinedFunction() {
        // Given
        String content = """
                key: $.key
                value: read_json($.value)
                timestamp: $.timestamp
                headers: $.headers
                where:
                  read_json:
                    op: io.kroxylicious.filter.record.manipulation.format.jackson3.DeserializeJson
                    format: json
                """;
        var config = new YAMLMapper().readValue(content, RecordTransformConfig2.class);

        // When/Then
        assertThatNoException().isThrownBy(() -> check.check(config));
    }

}