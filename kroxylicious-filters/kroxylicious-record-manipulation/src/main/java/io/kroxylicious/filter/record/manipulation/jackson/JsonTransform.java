/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.lang.reflect.Type;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.proxy.plugin.Plugin;

@Plugin(configType = JsonTransform.Config.class)
public class JsonTransform implements OpFactory<JsonNode, JsonNode> {

    static final ObjectMapper MAPPER = new ObjectMapper();

    public record Config(SchemaConfig schema) {}

    @Override
    public BaseTypedOp<JsonNode, JsonNode> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        Config c = MAPPER.convertValue(config, Config.class);
        return JacksonFunction.buildMask(c.schema(), lookup);
    }

}
