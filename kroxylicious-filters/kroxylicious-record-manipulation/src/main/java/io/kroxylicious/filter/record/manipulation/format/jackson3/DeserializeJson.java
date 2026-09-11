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

import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLReadFeature;

@Plugin(configType = DeserializeJson.JsonReaderConfig.class)
public class DeserializeJson implements OpFactory<ByteBuffer, Object> {

    public static final String FORMAT_PARAMETER = "format";
    public static final String FORMAT_VALUE_JSON = "json";
    public static final String FORMAT_VALUE_YAML = "yaml";
    public static final String FORMAT_VALUE_CSV = "csv";

    public static final String TYPE_PARAMETER = "type";
    public static final String TYPE_VALUE_JSON_NODE = JsonNode.class.getName();

    public static final String READ_FEATURES_PARAMETER = "readFeatures";
    public static final String DESERIALIZER_FEATURES_PARAMETER = "deserializerFeatures";


    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = FORMAT_PARAMETER)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = JsonReaderConfig.class, name = FORMAT_VALUE_JSON),
            @JsonSubTypes.Type(value = YamlReaderConfig.class, name = FORMAT_VALUE_YAML),
            @JsonSubTypes.Type(value = CsvReaderConfig.class, name = FORMAT_VALUE_CSV)
    })
    interface ReaderConfig {
        String type();

        ObjectReader createReader(JavaType javaType);
    }

    @JsonTypeName("json")
    public record JsonReaderConfig(
                                   String type,
                                   Map<String, Boolean> readFeatures,
                                   Map<String, Boolean> deserializationFeatures)
            implements ReaderConfig {
        @Override
        public ObjectReader createReader(JavaType javaType) {
            JsonMapper.Builder builder = JsonMapper.builder();
            FeatureConfigurations.asJacksonFeatureMap(readFeatures, JsonReadFeature.class).forEach(builder::configure);
            FeatureConfigurations.asConfigFeatureMap(deserializationFeatures, DeserializationFeature.class).forEach(builder::configure);
            return builder
                    .build().readerFor(javaType);
        }
    }

    @JsonTypeName("csv")
    public record CsvReaderConfig(
            String type,
            List<ColumnConfig> columnConfigs,
            Map<String, Boolean> readFeatures,
            Map<String, Boolean> deserializationFeatures)
            implements ReaderConfig {
        @Override
        public ObjectReader createReader(JavaType javaType) {
            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            for (ColumnConfig columnConfig : columnConfigs) {
                schemaBuilder = schemaBuilder.addColumn(columnConfig.name(), columnConfig.type(), c ->
                        c.withArrayElementSeparator(columnConfig.arrayElementSep()));
            }

            CsvMapper.Builder builder = CsvMapper.builder();
            FeatureConfigurations.asJacksonFeatureMap(readFeatures, CsvReadFeature.class).forEach(builder::configure);
            FeatureConfigurations.asConfigFeatureMap(deserializationFeatures, DeserializationFeature.class).forEach(builder::configure);
            CsvMapper mapper = builder.build();
            return mapper.reader(schemaBuilder.build()).forType(javaType);
        }
    }

    @JsonTypeName("yaml")
    public record YamlReaderConfig(
                                   String type,
                                   Map<String, Boolean> readFeatures,
                                   Map<String, Boolean> deserializationFeatures)
            implements ReaderConfig {
        @Override
        public ObjectReader createReader(JavaType javaType) {
            YAMLMapper.Builder builder = YAMLMapper.builder();
            FeatureConfigurations.asJacksonFeatureMap(readFeatures, YAMLReadFeature.class).forEach(builder::configure);
            FeatureConfigurations.asConfigFeatureMap(deserializationFeatures, DeserializationFeature.class).forEach(builder::configure);
            return builder.build().readerFor(javaType);
        }
    }

    @Override
    public BaseTypedOp<ByteBuffer, Object> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        ReaderConfig readerConfig = ConfigMapper.CONFIG_MAPPER.convertValue(configMap, ReaderConfig.class);

        JavaType javaType;
        String type = readerConfig.type();
        TypeFactory typeFactory = ConfigMapper.CONFIG_MAPPER.getTypeFactory();
        if (type != null) {
            javaType = typeFactory.constructFromCanonical(type);
        }
        else {
            javaType = typeFactory.constructType(JsonNode.class);
        }

        ObjectReader reader = readerConfig.createReader(javaType);
        var deserializer = new JacksonDeserializer(reader);

        // TODO We shouldn't pass a Jackson JavaType into BaseTypedOp
        // but that means we need to parse the Java type expression ourselves and
        // construct both a JavaType and a (vanilla) Type in a consistent way!

        return BaseTypedOp.of(ByteBuffer.class, (Type) javaType.getRawClass(), (value, opContext) -> deserializer.deserialize(value));
    }
}
