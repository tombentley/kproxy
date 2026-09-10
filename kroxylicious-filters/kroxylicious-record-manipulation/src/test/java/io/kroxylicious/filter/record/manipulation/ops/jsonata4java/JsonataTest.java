/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.jsonata4java;

import java.lang.reflect.Type;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.LongNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class JsonataTest {

    @Mock
    PluginLookup lookup;

    @Mock
    Type type;

    @Mock
    OpContext opContext;

    @AfterEach
    void afterEach() {
        Mockito.verifyNoInteractions(lookup);
        Mockito.verifyNoInteractions(type);
        Mockito.verifyNoInteractions(opContext);
    }

    @Test
    void createRejectsMissingExpression() {
        Jsonata jsonata = new Jsonata();
        Map<String, Object> config = Map.of();
        assertThatThrownBy(() -> jsonata.create(config, lookup, type))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'expression' property is required");
    }

    @Test
    void createRejectsWronglyTypedExpression() {
        Jsonata jsonata = new Jsonata();
        Map<String, Object> conf = Map.of(Jsonata.CONF_PARAM_EXPRESSION, 1);
        assertThatThrownBy(() -> jsonata.create(conf, lookup, type))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'expression' property must be a string, but was java.lang.Integer");
    }

    @Test
    void applyEvaluatesTheExpression() {
        Jsonata jsonata = new Jsonata();
        var op = jsonata.create(Map.of(Jsonata.CONF_PARAM_EXPRESSION, "$sum(c)"), lookup, type);


        var input = JsonMapper.builder().build().readTree("{ \"a\":1, \"b\":2, \"c\":[1,2,3,4,5] }");
        JsonNode apply = op.apply(input, opContext);


        assertThat(apply)
                .asInstanceOf(InstanceOfAssertFactories.type(LongNode.class))
                .extracting(LongNode::asLong)
                .as("The sum of the array property 'c' should be 15")
                .isEqualTo(15L);
    }
}
