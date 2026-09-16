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
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.proxy.plugin.Plugin;

import edu.umd.cs.findbugs.annotations.Nullable;
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

    /**
     * The source data format, e.g. {@code json}, {@code yaml} or {@code csv}.
     * Default is {@code json}.
     */
    public static final String FORMAT_PARAMETER = "format";
    public static final String FORMAT_VALUE_JSON = "json";
    public static final String FORMAT_VALUE_YAML = "yaml";
    public static final String FORMAT_VALUE_CSV = "csv";

    /**
     * The FQCN of the Java representation of the root node,
     * e.g. {@code tools.jackson.databind.JsonNode} (for Jackson default typing),
     * {@code java.lang.Object} (for Map/List typing)
     * or {@code com.example.your.class.Name for you own type}.
     * Default is {@code tools.jackson.databind.JsonNode}.
     */
    public static final String TYPE_PARAMETER = "type";
    public static final String TYPE_VALUE_JSON_NODE = JsonNode.class.getName();

    public static final String READ_FEATURES_PARAMETER = "readFeatures";
    public static final String DESERIALIZER_FEATURES_PARAMETER = "deserializerFeatures";

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = FORMAT_PARAMETER,
            defaultImpl = JsonReaderConfig.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = JsonReaderConfig.class, name = FORMAT_VALUE_JSON),
            @JsonSubTypes.Type(value = YamlReaderConfig.class, name = FORMAT_VALUE_YAML),
            @JsonSubTypes.Type(value = CsvReaderConfig.class, name = FORMAT_VALUE_CSV)
    })
    interface ReaderConfig {
        @Nullable String type();

        ObjectReader createReader(JavaType javaType);
    }

    @JsonTypeName("json")
    public record JsonReaderConfig(
                                   @Nullable String type,
                                   @Nullable Map<String, Boolean> readFeatures,
                                   @Nullable Map<String, Boolean> deserializationFeatures)
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
                                  @Nullable String type,
                                  @JsonProperty(required = true) List<ColumnConfig> columns,
                                  @Nullable Map<String, Boolean> readFeatures,
                                  @Nullable Map<String, Boolean> deserializationFeatures)
            implements ReaderConfig {

        public CsvReaderConfig {
            Map<String, List<ColumnConfig>> collect = columns.stream().collect(Collectors.groupingBy(ColumnConfig::name));
            for (var entry : collect.entrySet()) {
                if (entry.getValue().size() != 1) {
                    throw new IllegalArgumentException("Columns must have unique names, but there are " + entry.getValue().size()
                            + " columns with name: " + entry.getKey());
                }
            }
        }

        @Override
        public ObjectReader createReader(JavaType javaType) {
            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            for (ColumnConfig columnConfig : columns) {
                schemaBuilder = schemaBuilder.addColumn(columnConfig.name(), columnConfig.type(), c -> c.withArrayElementSeparator(columnConfig.arrayElementSep()));
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
    public BaseTypedOp<ByteBuffer, Object> create(@Nullable Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        ReaderConfig readerConfig;
        if (configMap == null) {
            readerConfig = new JsonReaderConfig(null, null, null);
        } else {
            readerConfig = ConfigMapper.CONFIG_MAPPER.convertValue(configMap, ReaderConfig.class);
        }

        JavaType javaType = getJavaType(readerConfig.type());
        ObjectReader reader = readerConfig.createReader(javaType);
        var deserializer = new JacksonDeserializer(reader);

        // TODO We shouldn't pass a Jackson JavaType into BaseTypedOp
        // but that means we need to parse the Java type expression ourselves and
        // construct both a JavaType and a (vanilla) Type in a consistent way!

        return BaseTypedOp.of(ByteBuffer.class, (Type) javaType.getRawClass(), (value, opContext) -> deserializer.deserialize(value));
    }

    static JavaType getJavaType(@Nullable String type) {
        JavaType javaType;
        TypeFactory typeFactory = ConfigMapper.CONFIG_MAPPER.getTypeFactory();
        if (type != null) {
            javaType = typeFactory.constructFromCanonical(type);
        }
        else {
            javaType = typeFactory.constructType(JsonNode.class);
        }
        return javaType;
    }
}
