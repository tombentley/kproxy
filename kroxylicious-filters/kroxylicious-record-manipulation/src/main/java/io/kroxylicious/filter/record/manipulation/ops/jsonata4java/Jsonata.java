/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.jsonata4java;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import com.api.jsonata4java.expressions.EvaluateException;
import com.api.jsonata4java.expressions.Expressions;
import com.api.jsonata4java.expressions.ParseException;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;

import tools.jackson.databind.JsonNode;

public class Jsonata implements OpFactory<JsonNode, JsonNode> {

    public static final String CONF_PARAM_EXPRESSION = "expression";

    @Override
    public BaseTypedOp<JsonNode, JsonNode> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        var expressions = createExpressions(config);

        return BaseTypedOp.of(JsonNode.class, JsonNode.class, (value, context) -> {
            try {
                return expressions.evaluate(value);
            }
            catch (EvaluateException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Expressions createExpressions(Map<String, Object> config) {
        Object exprObj = config.get(CONF_PARAM_EXPRESSION);
        if (exprObj instanceof String expression) {
            try {
                // TODO support the re2 regex engine
                return Expressions.parse(expression);
            }
            catch (ParseException | IOException e) {
                throw new IllegalArgumentException("The '" + CONF_PARAM_EXPRESSION + "' property contained invalid JSONata expression(s)", e);
            }
        }
        else if (exprObj != null) {
            throw new IllegalArgumentException("The '" + CONF_PARAM_EXPRESSION + "' property must be a string, but was " + exprObj.getClass().getName());
        }
        else {
            throw new IllegalArgumentException("The '" + CONF_PARAM_EXPRESSION + "' property is required");
        }
    }
}
