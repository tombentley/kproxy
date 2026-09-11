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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

import tools.jackson.core.json.JsonWriteFeature;
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "format")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SerializeJson.JsonWriterConfig.class, name = "json"),
            @JsonSubTypes.Type(value = SerializeJson.YamlWriterConfig.class, name = "yaml"),
            @JsonSubTypes.Type(value = SerializeJson.CsvWriterConfig.class, name = "csv")
    })
    interface WriterConfig {
        ObjectWriter createWriter();
    }

    @JsonTypeName("json")
    public record JsonWriterConfig(
            Map<String, Boolean> writeFeatures,
            Map<String, Boolean> serializationFeatures)
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
                                  List<ColumnConfig> columnConfigs,
                                  Map<String, Boolean> writeFeatures,
                                  Map<String, Boolean> serializationFeatures)
            implements WriterConfig {
        @Override
        public ObjectWriter createWriter() {
            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            for (ColumnConfig columnConfig : columnConfigs) {
                schemaBuilder = schemaBuilder.addColumn(columnConfig.name(), columnConfig.type(), c ->
                        c.withArrayElementSeparator(columnConfig.arrayElementSep()));
            }
            CsvMapper.Builder builder = CsvMapper.builder();
            FeatureConfigurations.asJacksonFeatureMap(writeFeatures, CsvWriteFeature.class).forEach(builder::configure);
            FeatureConfigurations.asConfigFeatureMap(serializationFeatures, SerializationFeature.class).forEach(builder::configure);
            return builder.build().writer(schemaBuilder.build());
        }
    }

    @JsonTypeName("yaml")
    public record YamlWriterConfig(
            boolean indentArrays,
            Map<String, Boolean> writeFeatures,
            Map<String, Boolean> serializationFeatures)
            implements WriterConfig {
        @Override
        public ObjectWriter createWriter() {
            YAMLMapper.Builder builder = YAMLMapper.builder();
            FeatureConfigurations.asJacksonFeatureMap(writeFeatures, YAMLWriteFeature.class).forEach(builder::configure);
            FeatureConfigurations.asConfigFeatureMap(serializationFeatures, SerializationFeature.class).forEach(builder::configure);
            return builder
                    .configure(YAMLWriteFeature.INDENT_ARRAYS, indentArrays())
                    .build().writer();
        }
    }

    @Override
    public BaseTypedOp<JsonNode, ByteBuffer> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        WriterConfig config1 = ConfigMapper.CONFIG_MAPPER.convertValue(config, WriterConfig.class);
        var d = new JacksonSerializer(config1.createWriter());
        // TODO plug in type parser
        return BaseTypedOp.of(JsonNode.class, ByteBuffer.class, (value, opContext) -> d.serialize(value));
    }
}
