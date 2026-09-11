/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.jsonschema.networknt.jackson3;

import java.lang.reflect.Type;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaValidateTest {

    @Mock
    PluginLookup pluginLookup;

    @Mock
    Type argumentType;

    @Mock
    OpContext opContext;

    @AfterEach
    void afterEach() {
        Mockito.verifyNoInteractions(pluginLookup, argumentType, opContext);
    }

    @Test
    void validationAcceptsValidJson() {
        var factory = new JsonSchemaValidate();
        var config = Map.<String, Object>of(
                "jsonSchema", """
                        {
                          "type": "object",
                          "properties": {
                            "foo": {
                              "type": "string"
                            }
                          }
                        }
                        """
        );
        var op = factory.create(config, pluginLookup, argumentType);
        assertThat(op.inputType()).isEqualTo(JsonNode.class);
        assertThat(op.outputType()).isEqualTo(JsonNode.class);
        JsonNode value = new JsonMapper().readTree("""
                {
                  "foo": "bar"
                }
                """);
        JsonNode applied = op.apply(value, opContext);
        assertThat(applied).isSameAs(value);
    }

    @Test
    void validationRejectsInvalidJson() {
        var factory = new JsonSchemaValidate();
        var config = Map.<String, Object>of(
                "jsonSchema", """
                        {
                          "type": "object",
                          "properties": {
                            "foo": {
                              "type": "string"
                            }
                          },
                          "additionalProperties": false
                        }
                        """
        );
        var op = factory.create(config, pluginLookup, argumentType);
        assertThat(op.inputType()).isEqualTo(JsonNode.class);
        assertThat(op.outputType()).isEqualTo(JsonNode.class);
        JsonNode value = new JsonMapper().readTree("""
                {"oops": true}
                """);
        assertThatThrownBy(() -> op.apply(value, opContext))
                .isExactlyInstanceOf(DataNotSchemaValidException.class)
                .hasMessageContaining("property 'oops' is not defined in the schema and the schema does not allow additional properties");
    }

    @Test
    void factoryRejectsSchemaNotJson() {
        var factory = new JsonSchemaValidate();
        var config = Map.<String, Object>of(
                "jsonSchema", """
                        this is not json
                        """
        );
        assertThatThrownBy(() -> factory.create(config, pluginLookup, argumentType))
                .isInstanceOf(SchemaNotValidException.class)
                .hasMessage("Given value for 'jsonSchema' was not valid JSON");

    }

    @Test
    void factoryRejectsSchemaNotValid() {
        var factory = new JsonSchemaValidate();
        var config = Map.<String, Object>of(
                "jsonSchema", """
                        ["mouse"]
                        """
        );
        assertThatThrownBy(() -> factory.create(config, pluginLookup, argumentType))
                .isInstanceOf(SchemaNotValidException.class)
                .hasMessage("Given value for 'jsonSchema' was not a valid JsonSchema: : array found, [object, boolean] expected");

    }

}