/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.jsonata.dashjoin;

import java.lang.reflect.Type;
import java.util.Map;

import com.dashjoin.jsonata.JException;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;

public class Jsonata implements OpFactory<Object, Object> {

    public static final String CONF_PARAM_EXPRESSION = "expression";

    @Override
    public BaseTypedOp<Object, Object> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        var expressions = createExpressions(config);

        return BaseTypedOp.of(Object.class, Object.class, (value, context) -> {
            try {
                return expressions.evaluate(value);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static com.dashjoin.jsonata.Jsonata createExpressions(Map<String, Object> config) {
        Object exprObj = config.get(CONF_PARAM_EXPRESSION);
        if (exprObj instanceof String expression) {
            try {
                com.dashjoin.jsonata.Jsonata jsonata = com.dashjoin.jsonata.Jsonata.jsonata(expression);
                // TODO jsonata.getErrors()
                // TODO jsonata.setValidateInput();
                // TODO jsonata.assign();
                // TODO jsonata.registerFunction();
                // TODO jsonata.setOutputConvertNulls();
                return jsonata;
            }
            catch (JException e) {
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
