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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLReadFeature;

@Plugin(configType = DeserializeJson.JsonReaderConfig.class)
public class DeserializeJson implements OpFactory<ByteBuffer, JsonNode> {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "format")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = JsonReaderConfig.class, name = "json"),
            @JsonSubTypes.Type(value = YamlReaderConfig.class, name = "yaml"),
            @JsonSubTypes.Type(value = CsvReaderConfig.class, name = "csv")
    })
    interface ReaderConfig {
        ObjectReader createReader();
    }

    @JsonTypeName("json")
    public record JsonReaderConfig(
                                   boolean allowJavaComments,
                                   boolean allowYamlComments,
                                   boolean allowSingleQuotes,
                                   boolean allowTrailingComma,
                                   boolean allowUnquotedProperty) {
        // TODO and the rest
        // or use a less verbose way to do this?
        ObjectReader createMapper() {
            return JsonMapper.builder()
                    .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, allowJavaComments())
                    .configure(JsonReadFeature.ALLOW_YAML_COMMENTS, allowYamlComments())
                    .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, allowSingleQuotes())
                    .configure(JsonReadFeature.ALLOW_TRAILING_COMMA, allowTrailingComma())
                    .configure(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES, allowUnquotedProperty())
                    .build().reader();
        }
    }

    record ColumnConfig(String name,
                        CsvSchema.ColumnType type) {

    }

    @JsonTypeName("csv")
    public record CsvReaderConfig(
                                  List<ColumnConfig> columnConfigs,
                                  boolean allowComments,
                                  boolean allowTrailingComma) {
        // TODO and the rest
        // or use a less verbose way to do this?
        ObjectReader createMapper() {
            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            for (ColumnConfig columnConfig : columnConfigs) {
                schemaBuilder = schemaBuilder.addColumn(columnConfig.name(), columnConfig.type());
            }
            return CsvMapper.builder()
                    .configure(CsvReadFeature.ALLOW_COMMENTS, allowComments())
                    .configure(CsvReadFeature.ALLOW_TRAILING_COMMA, allowTrailingComma())
                    .build().reader(schemaBuilder.build());
        }
    }

    @JsonTypeName("yaml")
    public record YamlReaderConfig(
                                   boolean parseOctalNumbers) {
        // TODO and the rest
        // or use a less verbose way to do this?
        ObjectReader createMapper() {
            return YAMLMapper.builder()
                    .configure(YAMLReadFeature.PARSE_OCTAL_NUMBERS, parseOctalNumbers())
                    .build().reader();
        }
    }

    @Override
    public BaseTypedOp<ByteBuffer, JsonNode> create(Map<String, Object> configMap, PluginLookup lookup, Type argumentType) {
        ReaderConfig readerConfig = ConfigMapper.CONFIG_MAPPER.convertValue(configMap, ReaderConfig.class);
        var deserializer = new JacksonDeserializer(readerConfig.createReader());
        // TODO not just JsonNode, we could make to Object/Map/List, or to some given Java type
        // TODO plug in type parser
        return BaseTypedOp.of(ByteBuffer.class, JsonNode.class, (value, opContext) -> deserializer.deserialize(value));
    }
}
