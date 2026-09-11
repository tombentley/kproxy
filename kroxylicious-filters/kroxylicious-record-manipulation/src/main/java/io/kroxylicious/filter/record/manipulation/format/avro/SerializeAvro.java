/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.avro.Schema;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Serializes an {@link AvroValue} to Avro's single-object binary encoding - the Avro equivalent of
 * {@link io.kroxylicious.filter.record.manipulation.format.jackson2.SerializeJson}, and the inverse of
 * {@link DeserializeAvro}.
 * <p>
 * Takes no schema config of its own - see {@link AvroValue}; it builds an {@link AvroBinarySerializer} from
 * whatever {@link Schema} arrives bundled with each value, and caches it against that schema (compared with
 * {@link Schema#equals(Object)}) so building a fresh {@link org.apache.avro.generic.GenericDatumWriter}
 * isn't repeated per record - mirrors the mask cache in {@link AvroTransform}.
 */
@Plugin(configType = Void.class)
public class SerializeAvro implements OpFactory<AvroValue, ByteBuffer> {

    @Override
    public BaseTypedOp<AvroValue, ByteBuffer> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        return BaseTypedOp.of(AvroValue.class, ByteBuffer.class, new CachingSerializer());
    }

    private static final class CachingSerializer implements BiFunction<AvroValue, OpContext, ByteBuffer> {
        private volatile Schema cachedSchema;
        private volatile AvroBinarySerializer cachedSerializer;

        @Override
        public ByteBuffer apply(AvroValue value, OpContext opContext) {
            Schema schema = value.schema();
            AvroBinarySerializer serializer = cachedSerializer;
            if (serializer == null || !schema.equals(cachedSchema)) {
                serializer = new AvroBinarySerializer(schema);
                cachedSchema = schema;
                cachedSerializer = serializer;
            }
            return serializer.serialize(value.value());
        }
    }
}
