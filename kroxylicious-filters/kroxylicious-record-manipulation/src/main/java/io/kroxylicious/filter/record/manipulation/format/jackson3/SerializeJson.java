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
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLReadFeature;

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
                                   boolean allowJavaComments,
                                   boolean allowYamlComments,
                                   boolean allowSingleQuotes,
                                   boolean allowTrailingComma,
                                   boolean allowUnquotedProperty)
            implements WriterConfig {
        // TODO and the rest
        // or use a less verbose way to do this?
        public ObjectWriter createWriter() {
            return JsonMapper.builder()
                    .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, allowJavaComments())
                    .configure(JsonReadFeature.ALLOW_YAML_COMMENTS, allowYamlComments())
                    .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, allowSingleQuotes())
                    .configure(JsonReadFeature.ALLOW_TRAILING_COMMA, allowTrailingComma())
                    .configure(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES, allowUnquotedProperty())
                    .build().writer();
        }
    }

    record ColumnConfig(String name,
                        CsvSchema.ColumnType type) {

    }

    @JsonTypeName("csv")
    public record CsvWriterConfig(
                                  List<DeserializeJson.ColumnConfig> columnConfigs,
                                  boolean allowComments,
                                  boolean allowTrailingComma)
            implements WriterConfig {
        // TODO and the rest
        // or use a less verbose way to do this?
        public ObjectWriter createWriter() {
            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            for (DeserializeJson.ColumnConfig columnConfig : columnConfigs) {
                schemaBuilder = schemaBuilder.addColumn(columnConfig.name(), columnConfig.type());
            }
            return CsvMapper.builder()
                    .configure(CsvReadFeature.ALLOW_COMMENTS, allowComments())
                    .configure(CsvReadFeature.ALLOW_TRAILING_COMMA, allowTrailingComma())
                    .build().writer(schemaBuilder.build());
        }
    }

    @JsonTypeName("yaml")
    public record YamlWriterConfig(
                                   boolean parseOctalNumbers)
            implements WriterConfig {
        // TODO and the rest
        // or use a less verbose way to do this?
        public ObjectWriter createWriter() {
            return YAMLMapper.builder()
                    .configure(YAMLReadFeature.PARSE_OCTAL_NUMBERS, parseOctalNumbers())
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
