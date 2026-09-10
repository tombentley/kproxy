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
import com.api.jsonata4java.expressions.ParseException;
import tools.jackson.databind.JsonNode;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;

import com.api.jsonata4java.expressions.Expressions;

public class Jsonata implements OpFactory<JsonNode, JsonNode> {
    @Override
    public BaseTypedOp<JsonNode, JsonNode> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        var expressions = extracted(config);

        return BaseTypedOp.of(JsonNode.class, JsonNode.class, (value, context) -> {
            try {
                return expressions.evaluate(value);
            }
            catch (EvaluateException e) {
                throw new RuntimeException(e);
            }
        });

    }

    private static Expressions extracted(Map<String, Object> config) {
        String expression = (String) config.get("expression");
        try {
            return Expressions.parse(expression);
        }
        catch (ParseException | IOException e) {
            throw new IllegalArgumentException("The 'expression' property contained invalid JSONata expression(s)", e);
        }
    }
}
