/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson3;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.proxy.plugin.Plugin;

import edu.umd.cs.findbugs.annotations.Nullable;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.CsvWriteFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

@Plugin(configType = SerializeJson.WriterConfig.class)
public class SerializeJson implements OpFactory<JsonNode, ByteBuffer> {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "format",
            defaultImpl = JsonWriterConfig.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SerializeJson.JsonWriterConfig.class, name = "json"),
            @JsonSubTypes.Type(value = SerializeJson.YamlWriterConfig.class, name = "yaml"),
            @JsonSubTypes.Type(value = SerializeJson.CsvWriterConfig.class, name = "csv")
    })
    interface WriterConfig {
        String type();
        ObjectWriter createWriter();
    }

    @JsonTypeName("json")
    public record JsonWriterConfig(
                                   @Nullable String type,
                                   @Nullable Map<String, Boolean> writeFeatures,
                                   @Nullable Map<String, Boolean> serializationFeatures)
            implements WriterConfig {
        @Override
        public ObjectWriter createWriter() {
            JsonMapper.Builder builder = JsonMapper.builder();
            builder.configure(SerializationFeature.INDENT_OUTPUT, true);
            FeatureConfigurations.asJacksonFeatureMap(writeFeatures, JsonWriteFeature.class).forEach(builder::configure);
            FeatureConfigurations.asConfigFeatureMap(serializationFeatures, SerializationFeature.class).forEach(builder::configure);
            return builder
                    .build().writer();
        }
    }

    @JsonTypeName("csv")
    public record CsvWriterConfig(
                                  @Nullable String type,
                                  @JsonProperty(required = true) List<ColumnConfig> columns,
                                  @Nullable Map<String, Boolean> writeFeatures,
                                  @Nullable Map<String, Boolean> serializationFeatures)
            implements WriterConfig {
        @Override
        public ObjectWriter createWriter() {
            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            for (ColumnConfig columnConfig : columns) {
                schemaBuilder = schemaBuilder.addColumn(columnConfig.name(), columnConfig.type(), c -> c.withArrayElementSeparator(columnConfig.arrayElementSep()));
            }
            CsvMapper.Builder builder = CsvMapper.builder();
            FeatureConfigurations.asJacksonFeatureMap(writeFeatures, CsvWriteFeature.class).forEach(builder::configure);
            FeatureConfigurations.asConfigFeatureMap(serializationFeatures, SerializationFeature.class).forEach(builder::configure);
            return builder.build().writer(schemaBuilder.build());
        }
    }

    @JsonTypeName("yaml")
    public record YamlWriterConfig(
                                   @Nullable String type,
                                   Map<String, Boolean> writeFeatures,
                                   Map<String, Boolean> serializationFeatures)
            implements WriterConfig {
        @Override
        public ObjectWriter createWriter() {
            YAMLMapper.Builder builder = YAMLMapper.builder();
            FeatureConfigurations.asJacksonFeatureMap(writeFeatures, YAMLWriteFeature.class).forEach(builder::configure);
            FeatureConfigurations.asConfigFeatureMap(serializationFeatures, SerializationFeature.class).forEach(builder::configure);
            return builder
                    .build().writer();
        }
    }

    @Override
    public BaseTypedOp<JsonNode, ByteBuffer> create(@Nullable Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        WriterConfig writerConfig;
        if (config == null) {
            writerConfig = new JsonWriterConfig(null, null, null);
        }
        else {
            writerConfig = ConfigMapper.CONFIG_MAPPER.convertValue(config, WriterConfig.class);
        }
        JavaType javaType = DeserializeJson.getJavaType(writerConfig.type());

        var d = new JacksonSerializer(writerConfig.createWriter());
        // TODO plug in type parser
        return BaseTypedOp.of((Type) javaType.getRawClass(),
                ByteBuffer.class, (value, opContext) -> d.serialize(value));
    }
}
