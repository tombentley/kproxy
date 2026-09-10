/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson3;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

@Plugin(configType = SerializeJson.Config.class)
public class SerializeJson implements OpFactory<JsonNode, ByteBuffer> {

    public record Config(boolean indentOutput,
                         boolean orderMapEntriesByKeys) {}

    @Override
    public BaseTypedOp<JsonNode, ByteBuffer> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        Config config1 = ConfigMapper.CONFIG_MAPPER.convertValue(config, Config.class);

        ObjectMapper mapper = JsonMapper.builder()
                .configure(SerializationFeature.INDENT_OUTPUT, config1.indentOutput())
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, config1.orderMapEntriesByKeys())
                .build();

        var d = new JacksonSerializer(mapper);
        return BaseTypedOp.of(JsonNode.class, ByteBuffer.class, (value, opContext) -> d.serialize(value));
    }
}
