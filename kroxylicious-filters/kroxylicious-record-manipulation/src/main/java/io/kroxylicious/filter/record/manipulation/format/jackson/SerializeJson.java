/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
        Config config1 = JsonTransform.MAPPER.convertValue(config, Config.class);

        ObjectMapper mapper = new ObjectMapper()
                .configure(SerializationFeature.INDENT_OUTPUT, config1.indentOutput())
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, config1.orderMapEntriesByKeys());

        var d = new JacksonSerializer(mapper);
        return BaseTypedOp.of(JsonNode.class, ByteBuffer.class, (value, opContext) -> d.serialize(value));
    }
}
