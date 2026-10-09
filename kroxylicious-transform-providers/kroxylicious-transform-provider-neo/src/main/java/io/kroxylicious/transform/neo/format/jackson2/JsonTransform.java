/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.format.jackson2;

import java.lang.reflect.Type;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpFactory;
import io.kroxylicious.transform.service.TransformInitializationContext;

@Plugin(configType = JsonTransform.Config.class)
public class JsonTransform implements OpFactory<JsonNode, JsonNode> {

    static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String SCHEMA_PARAMETER = "schema";

    public record Config(SchemaConfig schema) {}

    @Override
    public BaseTypedOp<JsonNode, JsonNode> create(Map<String, Object> config, TransformInitializationContext lookup, Type argumentType) {
        Config c = MAPPER.convertValue(config, Config.class);
        return JacksonFunction.buildMask(c.schema(), lookup);
    }

}
