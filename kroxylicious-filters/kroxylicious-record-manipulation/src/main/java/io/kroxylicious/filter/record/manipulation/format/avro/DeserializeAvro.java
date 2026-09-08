/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.avro.Schema;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Deserializes a record value/key from Avro's single-object binary encoding to an {@link AvroValue} -
 * the Avro equivalent of {@link io.kroxylicious.filter.record.manipulation.format.jackson.DeserializeJson}.
 * The only Avro op that parses schema config - see {@link AvroValue}.
 */
@Plugin(configType = DeserializeAvro.Config.class)
public class DeserializeAvro implements OpFactory<ByteBuffer, AvroValue> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record Config(String schema) {}

    @Override
    public BaseTypedOp<ByteBuffer, AvroValue> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        Config c = MAPPER.convertValue(config, Config.class);
        Schema schema = new Schema.Parser().parse(c.schema());
        var deserializer = new AvroBinaryDeserializer(schema);
        return BaseTypedOp.of(ByteBuffer.class, AvroValue.class, (value, opContext) -> new AvroValue(deserializer.deserialize(value), schema));
    }
}
