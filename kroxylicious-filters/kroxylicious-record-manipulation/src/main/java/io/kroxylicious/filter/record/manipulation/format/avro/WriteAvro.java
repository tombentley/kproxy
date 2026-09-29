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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import io.kroxylicious.filter.record.manipulation.format.Serializer;
import io.kroxylicious.filter.record.manipulation.format.jackson2.WriteJson;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.service.PluginLookup;

import edu.umd.cs.findbugs.annotations.Nullable;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Serializes an {@link AvroValue} to Avro's single-object binary encoding - the Avro equivalent of
 * {@link WriteJson}, and the inverse of
 * {@link ReadAvro}.
 * <p>
 * Takes no schema config of its own - see {@link AvroValue}; it builds an {@link AvroBinarySerializer} from
 * whatever {@link Schema} arrives bundled with each value, and caches it against that schema (compared with
 * {@link Schema#equals(Object)}) so building a fresh {@link org.apache.avro.generic.GenericDatumWriter}
 * isn't repeated per record - mirrors the mask cache in {@link AvroTransform}.
 */
@Plugin(configType = Void.class)
public class WriteAvro implements OpFactory<AvroValue, ByteBuffer> {

    private static final String FORMAT_PARAMETER = "format";
    private static final String FORMAT_VALUE_BINARY = "binary";
    private static final String FORMAT_VALUE_JSON = "json";

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = FORMAT_PARAMETER, defaultImpl = BinaryWriterConfig.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = BinaryWriterConfig.class, name = FORMAT_VALUE_BINARY),
            @JsonSubTypes.Type(value = JsonWriterConfig.class, name = FORMAT_VALUE_JSON)
    })
    interface WriterConfig {

        Serializer<Object> createSerializer(Schema schema);

        // @Nullable
        // String type(); // TODO Generic vs Specific

    }

    @JsonTypeName("binary")
    public record BinaryWriterConfig(
                                     @Nullable String type)
            implements WriterConfig {

        @Override
        public AvroBinarySerializer createSerializer(Schema schema) {
            return new AvroBinarySerializer(schema);
        }
    }

    @JsonTypeName("json")
    public record JsonWriterConfig(
                                   @Nullable String type)
            implements WriterConfig {

        @Override
        public Serializer createSerializer(Schema schema) {
            return new AvroJsonSerializer(schema);
        }
    }

    @Override
    public BaseTypedOp<AvroValue, ByteBuffer> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        WriterConfig readerConfig;

        tools.jackson.databind.ObjectMapper objectMapper = YAMLMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .build();
        readerConfig = objectMapper.convertValue(configMap, WriterConfig.class);

        return BaseTypedOp.of(AvroValue.class, ByteBuffer.class, new CachingSerializer(readerConfig));
    }

    private static final class CachingSerializer implements BiFunction<AvroValue, OpContext, ByteBuffer> {
        private final WriterConfig writerConfig;
        private volatile Schema cachedSchema;
        private volatile Serializer<Object> cachedSerializer;

        CachingSerializer(WriterConfig writerConfig) {
            this.writerConfig = writerConfig;
        }

        @Override
        public ByteBuffer apply(AvroValue value, OpContext opContext) {
            Schema schema = value.schema();
            Serializer<Object> serializer = cachedSerializer;
            if (serializer == null || !schema.equals(cachedSchema)) {
                serializer = writerConfig.createSerializer(schema);
                cachedSchema = schema;
                cachedSerializer = serializer;
            }
            return serializer.serialize(value.value());
        }
    }
}
