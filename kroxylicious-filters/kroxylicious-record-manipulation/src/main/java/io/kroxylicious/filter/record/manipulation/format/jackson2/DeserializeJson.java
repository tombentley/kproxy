/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson2;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

@Plugin(configType = DeserializeJson.Config.class)
public class DeserializeJson implements OpFactory<ByteBuffer, JsonNode> {

    public record Config(
                         boolean allowComments,
                         boolean allowSingleQuotes,
                         boolean allowTrailingComma,
                         boolean allowUnquotedFieldNames) {
        // TODO and the readFeatures
        // or use a less verbose way to do this?
    }

    @Override
    public BaseTypedOp<ByteBuffer, JsonNode> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        Config deserializationConfig = JsonTransform.MAPPER.convertValue(configMap, Config.class);
        ObjectMapper deserializationMapper = new ObjectMapper()
                .configure(JsonParser.Feature.ALLOW_COMMENTS, deserializationConfig.allowComments())
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, deserializationConfig.allowSingleQuotes())
                .configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), deserializationConfig.allowTrailingComma())
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, deserializationConfig.allowUnquotedFieldNames());
        var deserializer = new JacksonDeserializer(deserializationMapper);
        return BaseTypedOp.of(ByteBuffer.class, JsonNode.class, (value, opContext) -> deserializer.deserialize(value));
    }
}
