/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.neo.format.avro;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.avro.Schema;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import io.kroxylicious.filter.record.manipulation.neo.format.Deserializer;
import io.kroxylicious.filter.record.manipulation.neo.format.jackson3.ReadJson;
import io.kroxylicious.filter.record.manipulation.neo.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.neo.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.neo.type.SchemaType;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.service.PluginLookup;

import edu.umd.cs.findbugs.annotations.Nullable;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Deserializes a record value/key from Avro's single-object binary encoding to an {@link AvroValue} -
 * the Avro equivalent of {@link ReadJson}.
 * The only Avro op that parses schema config - see {@link AvroValue}.
 */
@Plugin(configType = ReadAvro.ReaderConfig.class)
public class ReadAvro implements OpFactory<ByteBuffer, AvroValue> {

    private static final String FORMAT_PARAMETER = "format";
    private static final String FORMAT_VALUE_BINARY = "binary";
    private static final String FORMAT_VALUE_JSON = "json";

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = FORMAT_PARAMETER, defaultImpl = BinaryReaderConfig.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = BinaryReaderConfig.class, name = FORMAT_VALUE_BINARY),
            @JsonSubTypes.Type(value = JsonReaderConfig.class, name = FORMAT_VALUE_JSON)
    })
    interface ReaderConfig {

        String schema();

        Deserializer createDeserializer(Schema schema);

        // @Nullable
        // String type(); // TODO Generic vs Specific

    }

    @JsonTypeName("binary")
    public record BinaryReaderConfig(
                                     String schema,
                                     @Nullable String type)
            implements ReaderConfig {

        @Override
        public AvroBinaryDeserializer createDeserializer(Schema schema) {
            return new AvroBinaryDeserializer(schema);
        }
    }

    @JsonTypeName("json")
    public record JsonReaderConfig(
                                   String schema,
                                   @Nullable String type)
            implements ReaderConfig {

        @Override
        public AvroJsonDeserializer createDeserializer(Schema schema) {
            return new AvroJsonDeserializer(schema);
        }
    }

    @Override
    public BaseTypedOp<ByteBuffer, AvroValue> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        ReaderConfig readerConfig;

        tools.jackson.databind.ObjectMapper objectMapper = YAMLMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .build();
        readerConfig = objectMapper.convertValue(configMap, ReaderConfig.class);

        var parsedSchema = new Schema.Parser().parse(readerConfig.schema());
        Deserializer deserializer = readerConfig.createDeserializer(parsedSchema);

        return BaseTypedOp.<ByteBuffer, AvroValue> of(ByteBuffer.class, AvroValue.class,
                (value, opContext) -> new AvroValue(deserializer.deserialize(value), parsedSchema),
                SchemaType.of(new AvroGenericTypeSystem(), parsedSchema));
    }
}
