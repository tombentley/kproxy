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
import org.apache.avro.generic.GenericRecord;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Deserializes a record value/key from Avro's single-object binary encoding to a {@link GenericRecord} -
 * the Avro equivalent of {@link io.kroxylicious.filter.record.manipulation.format.jackson.DeserializeJson}.
 */
@Plugin(configType = DeserializeAvro.Config.class)
public class DeserializeAvro implements OpFactory<ByteBuffer, GenericRecord> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record Config(String schema) {}

    @Override
    public BaseTypedOp<ByteBuffer, GenericRecord> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        Config c = MAPPER.convertValue(config, Config.class);
        Schema schema = new Schema.Parser().parse(c.schema());
        var deserializer = new AvroBinaryDeserializer(schema);
        return new StaticTypedOp<ByteBuffer, GenericRecord>() {
            @Override
            public Type outputType(Type inputType) {
                return GenericRecord.class;
            }

            @Override
            public GenericRecord apply(ByteBuffer value, OpContext opContext) {
                return deserializer.deserialize(value);
            }
        };
    }
}
