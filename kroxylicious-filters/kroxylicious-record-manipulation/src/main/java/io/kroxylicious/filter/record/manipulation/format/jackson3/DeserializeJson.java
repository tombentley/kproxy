/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson3;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

@Plugin(configType = DeserializeJson.Config.class)
public class DeserializeJson implements OpFactory<ByteBuffer, JsonNode> {

    public record Config(
                         boolean allowJavaComments,
                         boolean allowYamlComments,
                         boolean allowSingleQuotes,
                         boolean allowTrailingComma,
                         boolean allowUnquotedProperty) {
        // TODO and the rest
        // or use a less verbose way to do this?
    }

    @Override
    public BaseTypedOp<ByteBuffer, JsonNode> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config deserializationConfig = ConfigMapper.CONFIG_MAPPER.convertValue(configMap, Config.class);
        ObjectMapper deserializationMapper = JsonMapper.builder()
                .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, deserializationConfig.allowJavaComments())
                .configure(JsonReadFeature.ALLOW_YAML_COMMENTS, deserializationConfig.allowYamlComments())
                .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, deserializationConfig.allowSingleQuotes())
                .configure(JsonReadFeature.ALLOW_TRAILING_COMMA, deserializationConfig.allowTrailingComma())
                .configure(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES, deserializationConfig.allowUnquotedProperty())
                .build();
        var deserializer = new JacksonDeserializer(deserializationMapper);
        return BaseTypedOp.of(ByteBuffer.class, JsonNode.class, (value, opContext) -> deserializer.deserialize(value));
    }
}
