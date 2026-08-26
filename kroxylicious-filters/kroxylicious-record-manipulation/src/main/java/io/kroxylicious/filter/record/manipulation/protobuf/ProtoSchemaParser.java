/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.protobuf;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.OptionElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.TypeElement;

import io.apicurio.registry.utils.protobuf.schema.FileDescriptorUtils;
import io.apicurio.registry.utils.protobuf.schema.ProtobufFile;

import io.kroxylicious.filter.record.manipulation.config.OpConfig;

/**
 * Parses raw {@code .proto} IDL source text into a {@link ParsedProtoSchema} - the Protobuf equivalent of
 * {@code new Schema.Parser().parse(json)} for Avro.
 * <p>
 * Reuses {@link ProtobufFile#toProtoFileElement(String)} and {@link FileDescriptorUtils#toDescriptor(String,
 * ProtoFileElement, Map)} (both pure in-memory, no {@code protoc}, no network I/O) rather than parsing the
 * schema or converting it to a {@link Descriptors.FileDescriptor} ourselves - this repo's own
 * {@code kroxylicious-record-validation} module already exercises the same conversion in production. This
 * class only adds the one thing that conversion doesn't do: reading the non-standard {@code apply} keyword
 * off the schema's own option syntax (see {@link ParsedProtoSchema}).
 */
public final class ProtoSchemaParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String APPLY_OPTION = "apply";

    private ProtoSchemaParser() {
    }

    /**
     * Parses a {@code .proto} file and selects one of its message types as the schema root.
     * @param protoText the raw {@code .proto} IDL source, as a single file with no {@code import}s
     * @param rootMessageName the name of the top-level message type this schema describes
     * @return the root message's descriptor, plus every {@code apply} chain declared in the schema
     */
    public static ParsedProtoSchema parse(String protoText, String rootMessageName) {
        ProtoFileElement fileElement = ProtobufFile.toProtoFileElement(protoText);
        Descriptors.Descriptor rootDescriptor = FileDescriptorUtils.toDescriptor(rootMessageName, fileElement, Map.of());
        Map<Descriptors.GenericDescriptor, List<OpConfig>> apply = new HashMap<>();
        for (TypeElement type : fileElement.getTypes()) {
            if (type instanceof MessageElement message) {
                Descriptors.Descriptor descriptor = rootDescriptor.getFile().findMessageTypeByName(message.getName());
                if (descriptor != null) {
                    collectApply(message, descriptor, apply);
                }
            }
        }
        return new ParsedProtoSchema(rootDescriptor, apply);
    }

    private static void collectApply(MessageElement message, Descriptors.Descriptor descriptor, Map<Descriptors.GenericDescriptor, List<OpConfig>> out) {
        applyConfig(message.getOptions()).ifPresent(config -> out.put(descriptor, config));
        for (FieldElement field : message.getFields()) {
            Descriptors.FieldDescriptor fieldDescriptor = descriptor.findFieldByName(field.getName());
            if (fieldDescriptor != null) {
                applyConfig(field.getOptions()).ifPresent(config -> out.put(fieldDescriptor, config));
            }
        }
        for (TypeElement nested : message.getNestedTypes()) {
            if (nested instanceof MessageElement nestedMessage) {
                Descriptors.Descriptor nestedDescriptor = descriptor.findNestedTypeByName(nestedMessage.getName());
                if (nestedDescriptor != null) {
                    collectApply(nestedMessage, nestedDescriptor, out);
                }
            }
        }
    }

    /**
     * Reads every {@code option (apply) = {...};} declared on one field/message. Deliberately collects
     * every matching option (not just the first, unlike Apicurio's own {@code findOption}), since a
     * composed {@code apply} chain is written as one option occurrence per operation - mirrors a field's
     * {@code apply} being a YAML/JSON list for Avro/JSON.
     */
    private static Optional<List<OpConfig>> applyConfig(List<OptionElement> options) {
        List<Object> raw = options.stream()
                .filter(option -> APPLY_OPTION.equals(option.getName()))
                .map(OptionElement::getValue)
                .map(ProtoSchemaParser::normalize)
                .toList();
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(MAPPER.convertValue(raw, new TypeReference<List<OpConfig>>() {
        }));
    }

    /**
     * Wire represents a number/boolean option literal nested inside a map/list as an
     * {@link OptionElement.OptionPrimitive} (its {@code kind} plus the literal's raw text), rather than a
     * plain {@link Integer}/{@link Boolean} - unlike a bare string literal, which Wire already hands back
     * as a plain {@link String} (see the dump in this class's tests/history). Jackson has no deserializer
     * for Wire's internal type, so every option value is normalized into a plain
     * {@link Map}/{@link List}/{@link String}/{@link Number}/{@link Boolean} tree before conversion -
     * the same shape {@link com.fasterxml.jackson.databind.ObjectMapper} would produce reading JSON.
     */
    private static Object normalize(Object value) {
        if (value instanceof OptionElement.OptionPrimitive primitive) {
            String raw = primitive.getValue().toString();
            return switch (primitive.getKind()) {
                case BOOLEAN -> Boolean.parseBoolean(raw);
                case NUMBER -> raw.contains(".") || raw.contains("e") || raw.contains("E") ? Double.parseDouble(raw) : Long.parseLong(raw);
                default -> raw;
            };
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> result.put((String) key, normalize(mapValue)));
            return result;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(ProtoSchemaParser::normalize).toList();
        }
        return value;
    }
}
