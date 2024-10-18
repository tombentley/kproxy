/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class ConfigSchemaTest {

    @Test
    void shouldThrowWhenCreateWithInvalidSchema() throws IOException {
        assertThatThrownBy(() -> ConfigSchema.create("type: notLegal")).hasMessageContaining("""
                Not a valid schema: [/type: does not have a value in the enumeration ["array", "boolean", "integer", "null", "number", "object", "string"]""");
    }

    @Test
    void shouldAcceptValidSchema() throws IOException {
        assertThat(ConfigSchema.create("type: string")).isNotNull();
    }

    @Test
    void shouldValidateConfig() throws IOException {
        ConfigSchema configSchema = ConfigSchema.create("type: integer");
        configSchema.validateConfig("1");
        assertThatThrownBy(() -> configSchema.validateConfig("hello"))
                .hasMessage("Invalid config: [$: string found, integer expected]");
    }

}