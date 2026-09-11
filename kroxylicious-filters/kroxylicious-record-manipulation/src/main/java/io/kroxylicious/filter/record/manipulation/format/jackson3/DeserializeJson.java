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

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

import tools.jackson.core.json.JsonReadFeature;
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "format")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = JsonReaderConfig.class, name = "json"),
            @JsonSubTypes.Type(value = YamlReaderConfig.class, name = "yaml"),
            @JsonSubTypes.Type(value = CsvReaderConfig.class, name = "csv")
    })
    interface ReaderConfig {
        String type();
        ObjectReader createReader(JavaType javaType);
    }

    @JsonTypeName("json")
    public record JsonReaderConfig(
            String type,
                                   boolean allowJavaComments,
                                   boolean allowYamlComments,
                                   boolean allowSingleQuotes,
                                   boolean allowTrailingComma,
                                   boolean allowUnquotedProperty) implements ReaderConfig {
        // TODO and the rest
        // or use a less verbose way to do this?
        public ObjectReader createReader(JavaType javaType) {
            return JsonMapper.builder()
                    .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, allowJavaComments())
                    .configure(JsonReadFeature.ALLOW_YAML_COMMENTS, allowYamlComments())
                    .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, allowSingleQuotes())
                    .configure(JsonReadFeature.ALLOW_TRAILING_COMMA, allowTrailingComma())
                    .configure(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES, allowUnquotedProperty())
                    .build().readerFor(javaType);
        }
    }

    record ColumnConfig(String name,
                        CsvSchema.ColumnType type) {

    }

    @JsonTypeName("csv")
    public record CsvReaderConfig(
            String type,
                                  List<ColumnConfig> columnConfigs,
                                  boolean allowComments,
                                  boolean allowTrailingComma) implements ReaderConfig {
        // TODO and the rest
        // or use a less verbose way to do this?
        public ObjectReader createReader(JavaType javaType) {
            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            for (ColumnConfig columnConfig : columnConfigs) {
                schemaBuilder = schemaBuilder.addColumn(columnConfig.name(), columnConfig.type());
            }

            CsvMapper mapper = CsvMapper.builder()
                    .configure(CsvReadFeature.ALLOW_COMMENTS, allowComments())
                    .configure(CsvReadFeature.ALLOW_TRAILING_COMMA, allowTrailingComma())
                    .build();
            return mapper.reader(schemaBuilder.build());
        }
    }

    @JsonTypeName("yaml")
    public record YamlReaderConfig(
            String type,
            boolean parseOctalNumbers) implements ReaderConfig {
        // TODO and the rest
        // or use a less verbose way to do this?
        public ObjectReader createReader(JavaType javaType) {
            return YAMLMapper.builder()
                    .configure(YAMLReadFeature.PARSE_OCTAL_NUMBERS, parseOctalNumbers())
                    .build().readerFor(javaType);
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
        return BaseTypedOp.of(ByteBuffer.class, javaType, (value, opContext) -> deserializer.deserialize(value));
    }
}
