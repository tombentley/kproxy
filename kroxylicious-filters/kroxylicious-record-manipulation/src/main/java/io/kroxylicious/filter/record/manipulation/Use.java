/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.kafka.common.record.Record;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;

import io.kroxylicious.filter.record.manipulation.common.Ints;
import io.kroxylicious.filter.record.manipulation.common.Pipeline;
import io.kroxylicious.filter.record.manipulation.common.Strings;
import io.kroxylicious.filter.record.manipulation.config.MaskConfig;
import io.kroxylicious.filter.record.manipulation.jackson.ArrayNodes;
import io.kroxylicious.filter.record.manipulation.jackson.Jackson;
import io.kroxylicious.filter.record.manipulation.jackson.JacksonDeserializer;
import io.kroxylicious.filter.record.manipulation.jackson.ObjectNodes;

public class Use {

    private static final YAMLMapper MAPPER = new YAMLMapper();

    public static void main(String[] args) throws JsonProcessingException, NoSuchMethodException {
        var data = """
                firstName: Harry
                surname: Potter
                aliases:
                - "Vernon Dudley"
                - "Barny Weasley"
                ageYears: 17
                address:
                  streetAddress: Hogwarts
                  city: Hogsmead
                """;

        var dataTree = MAPPER.readTree(data);

        var maskContent = """
        type: object
        properties:
          firstName:
            type: string
            value: "REDACTED"
          #surname:
          #  type: string
          #  choose:
          #    - Smith
          #    - Jones
          aliases:
            type: array
            items:
              type: string
              random:
                minLength: 3
                maxLength: 15
                alphabet: abcdef ghijklmnopqrst uvwxyz
          ageYears:
            type: integer
            random:
              min: 18
              max: 100
          address:
            type: object
            properties:
              streetAddress:
                type: string
                hmac:
                  keyId: FOO
              city:
                type: string
                encrypt:
                  keyId: FOO
        """;
        // The above assumes that every node has a singular `type`.
        // That's fine so long as things like `random` work with multiple types
        // TODO How do we model deletion of property (or addition if it's not already present)?
        MaskConfig maskTree = MAPPER.readValue(maskContent, MaskConfig.class);
        MaskConfig unmaskTree = MAPPER.readValue(maskContent.replace("encrypt", "decrypt"), MaskConfig.class);

        Function<Record, ByteBuffer> keyExtractor = new KafkaRecordKeyExtractor();
        Function<ByteBuffer, JsonNode> deserializer = new JacksonDeserializer(MAPPER);
        Function<? super JsonNode, ? extends JsonNode> maskFn = buildMask(maskTree);
        var c = GenericTypeReflector.getExactReturnType(Use.class.getDeclaredMethod("buildMask", MaskConfig.class), Use.class);
        TypeFactory.parameterizedClass(Function.class, JsonNode.class, JsonNode.class);
        new Pipeline(List.of(keyExtractor, deserializer));
        // TODO serializer that works with Kafka records

        var result = maskFn.apply(dataTree);
        String x = MAPPER.writeValueAsString(result);
        System.out.println(x);

        Function<? super JsonNode, ? extends JsonNode> unmaskFn = buildMask(unmaskTree);
        var result2 = unmaskFn.apply(result);
        String y = MAPPER.writeValueAsString(result2);
        System.out.println(y);

        Supplier<? extends JsonNode> supplierFn = buildGenerator(maskTree);
        var generatedResult = supplierFn.get();
        System.out.println(MAPPER.writeValueAsString(generatedResult));

    }

    static <R, T> Function<T, R> toFn(Supplier<R> s) {
        return (T t) -> s.get();
    }

    private static Supplier<? extends JsonNode> buildGenerator(MaskConfig maskTree) {
        byte[] key = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};
        // TODO Key Mgmt
        switch (maskTree.type()) {
            case "string" -> {
                if (maskTree.value() !=  null) {
                    return Jackson.convertString(new Strings(key).value(maskTree.value().textValue()));
                }
                else if (maskTree.random() != null) {
                    return Jackson.convertString(new Strings(key).random(maskTree.random().minLength(), maskTree.random().maxLength(), maskTree.random().alphabet()));
                }
                else if (maskTree.choose() != null) {
                    return Jackson.convertString(new Strings(key).choose(maskTree.choose().stream().map(x -> (String) x).collect(Collectors.toSet())));
                }
                else {
                    return () -> new TextNode("");
                }
            }
            case "integer" -> {
                if (maskTree.value() !=  null) {
                    return Jackson.convertInt(new Ints().value(maskTree.value().intValue()));
                }
                else if (maskTree.random() != null) {
                    return Jackson.convertInt(new Ints().random(maskTree.random().min(), maskTree.random().max()));
                }
                else if (maskTree.choose() != null) {
                    return Jackson.convertInt(new Ints().choose(maskTree.choose().stream().map(x -> (Integer) x).collect(Collectors.toSet())));
                }
                else {
                    return () -> new IntNode(0);
                }
            }
            case "array" -> {
                if (maskTree.items() != null) {
                    return new ArrayNodes(MAPPER.getNodeFactory()).items2(buildGenerator(maskTree.items()));
                }
                else {
                    return () -> new ArrayNode(null);
                }
            }
            case "object" -> {
                if (maskTree.properties() !=  null) {
                    var mapping = maskTree.properties().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> buildGenerator(e.getValue())));
                    return new ObjectNodes(MAPPER.getNodeFactory()).mapProperties2((Map) mapping);
                }
                else {
                    return () -> new ObjectNode(null);
                }
            }
            default -> {
                throw new IllegalArgumentException("Invalid mask type: " + maskTree.type());
            }
        }
    }

    private static Function<? super JsonNode, ? extends JsonNode> buildMask(MaskConfig maskTree) {
        byte[] key = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};

        switch (maskTree.type()) {
            case "string" -> {
                if (maskTree.value() !=  null) {
                    return toFn(Jackson.convertString(new Strings(key).value(maskTree.value().textValue())));
                }
                else if (maskTree.random() != null) {
                    return toFn(Jackson.convertString(new Strings(key).random(maskTree.random().minLength(), maskTree.random().maxLength(), maskTree.random().alphabet())));
                }
                else if (maskTree.choose() != null) {
                    return toFn(Jackson.convertString(new Strings(key).choose(maskTree.choose().stream().map(x -> (String) x).collect(Collectors.toSet()))));
                }
                else if (maskTree.hmac() != null) {
                    return Jackson.convertString(new Strings(key).hmac());
                }
                else if (maskTree.encrypt() != null) {
                    return Jackson.convertString(new Strings(key).encrypt());
                }
                else if (maskTree.decrypt() != null) {
                    return Jackson.convertString(new Strings(key).decrypt());
                }
                else {
                    return Function.identity();
                }
            }
            case "integer" -> {
                if (maskTree.value() !=  null) {
                    return toFn(Jackson.convertInt(new Ints().value(maskTree.value().intValue())));
                }
                else if (maskTree.random() != null) {
                    return toFn(Jackson.convertInt(new Ints().random(maskTree.random().min(), maskTree.random().max())));
                }
                else if (maskTree.choose() != null) {
                    return toFn(Jackson.convertInt(new Ints().choose(maskTree.choose().stream().map(x -> (Integer) x).collect(Collectors.toSet()))));
                }
                else {
                    return Function.identity();
                }
            }
            case "array" -> {
                if (maskTree.items() != null) {
                    return new ArrayNodes(MAPPER.getNodeFactory()).items((Function) buildMask(maskTree.items()));
                }
                else {
                    return Function.identity();
                }
            }
            case "object" -> {
                if (maskTree.properties() !=  null) {
                    var mapping = maskTree.properties().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> buildMask(e.getValue())));
                    return new ObjectNodes(MAPPER.getNodeFactory()).mapProperties((Map) mapping);
                }
                else {
                    return Function.identity();
                }
            }
            default -> {
                throw new IllegalArgumentException("Invalid mask type: " + maskTree.type());
            }
        }
        //throw new IllegalArgumentException("Invalid mask type: " + maskTree);
    }

    private static class KafkaRecordKeyExtractor implements Function<Record, ByteBuffer> {
        @Override
        public ByteBuffer apply(Record r) {
            return r.key();
        }
    }

    private static class KafkaRecordValueExtractor implements Function<Record, ByteBuffer> {
        @Override
        public ByteBuffer apply(Record r) {
            return r.value();
        }
    }
}
