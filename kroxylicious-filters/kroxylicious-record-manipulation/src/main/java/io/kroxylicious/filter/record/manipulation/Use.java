/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.kafka.common.record.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;

import io.kroxylicious.filter.record.manipulation.common.ChooseIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.ChooseStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.ConstantIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.ConstantStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.Functional;
import io.kroxylicious.filter.record.manipulation.common.Pipeline;
import io.kroxylicious.filter.record.manipulation.common.RandomIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.RandomStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.Strings;
import io.kroxylicious.filter.record.manipulation.config.MaskConfig;
import io.kroxylicious.filter.record.manipulation.jackson.ArrayNodes;
import io.kroxylicious.filter.record.manipulation.jackson.Jackson;
import io.kroxylicious.filter.record.manipulation.jackson.JacksonDeserializer;
import io.kroxylicious.filter.record.manipulation.jackson.ObjectNodes;

public class Use {

    private static final Logger LOGGER = LoggerFactory.getLogger(Use.class);
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

        /*
         * TODO what is `type`? If it were really JSON Schema's `type` then we should be able to write this:
         *
         * properties:
         * creditCardNumber:
         * type: [integer, string]
         * value: 0000000000000000
         *
         * What would this mean?
         *
         * Or is `type` really a guard on the node type. In that case we should be able to write
         *
         * properties:
         * creditCardNumber:
         * - type: integer
         * value: 0000000000000000
         * - type: string
         * value: 0000 0000 0000 0000
         *
         * Which if more flexible and enumerates the cases.
         *
         * Or we reify the type into the `value` property name (i.e. all masks fan out by the json types):
         * properties:
         * creditCardNumber:
         * type: [integer, string]
         * integerValue: 0000000000000000
         * stringValue: 0000 0000 0000 0000
         *
         * But we only use type when constructing the mask Function. That's good, in the sense that once we've constructed
         * a mask function we can be reasonably sure that it will result in something that's structurally correct.
         * Indeed, it seems to be more or less necessary for _generation_ (well, I suppose we could infer the allowed types
         * from the keywords)
         *
         * But the alternative would be to figure it out at application-time.
         * If we see an `object` node then we apply the relevant masks for objects
         * If we see an `array` node then honour `items` etc.
         * But what about masks like choose -- should we filter those for the runtime type?
         */
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
        String masked = MAPPER.writeValueAsString(result);
        LOGGER.atInfo().addKeyValue("masked", masked).log("applied mask");

        Function<? super JsonNode, ? extends JsonNode> unmaskFn = buildMask(unmaskTree);
        var result2 = unmaskFn.apply(result);
        String unmasked = MAPPER.writeValueAsString(result2);
        LOGGER.atInfo().addKeyValue("unmasked", unmasked).log("applied unmask");

        Supplier<? extends JsonNode> supplierFn = buildGenerator(maskTree);
        var generatedResult = supplierFn.get();
        String generated = MAPPER.writeValueAsString(generatedResult);
        LOGGER.atInfo().addKeyValue("generated", generated).log("generated data");

    }

    private static Supplier<? extends JsonNode> buildGenerator(MaskConfig maskTree) {
        byte[] key = new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };
        // TODO Key Mgmt
        switch (maskTree.type()) {
            // TODO "null", "boolean", "number"
            case "string" -> {
                if (maskTree.value() != null) {
                    new Strings(key);
                    return Jackson.convertString(new ConstantStringSupplier(maskTree.value().textValue()));
                }
                else if (maskTree.random() != null) {
                    new Strings(key);
                    return Jackson.convertString(
                            new RandomStringSupplier(new Random(), maskTree.random().alphabet(), maskTree.random().minLength(), maskTree.random().maxLength()));
                }
                else if (maskTree.choose() != null) {
                    Set<String> from = maskTree.choose().stream().map(x -> (String) x).collect(Collectors.toSet());
                    return Jackson.convertString(new ChooseStringSupplier(new Random(), from));
                }
                else {
                    return () -> new TextNode("");
                }
            }
            case "integer" -> {
                if (maskTree.value() != null) {
                    return Jackson.convertInt(new ConstantIntSupplier(maskTree.value().intValue()));
                }
                else if (maskTree.random() != null) {
                    return Jackson.convertInt(new RandomIntSupplier(new Random(), maskTree.random().min(), maskTree.random().max()));
                }
                else if (maskTree.choose() != null) {
                    Set<Integer> from = maskTree.choose().stream().map(x -> (Integer) x).collect(Collectors.toSet());
                    return Jackson.convertInt(new ChooseIntSupplier(new Random(), from));
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
                if (maskTree.properties() != null) {
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
        byte[] key = new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };

        switch (maskTree.type()) {
            case "string" -> {
                if (maskTree.value() != null) {
                    return Functional.asFunction(Jackson.convertString(new ConstantStringSupplier(maskTree.value().textValue())));
                }
                else if (maskTree.random() != null) {
                    return Functional.asFunction(Jackson.convertString(
                            new RandomStringSupplier(new Random(), maskTree.random().alphabet(), maskTree.random().minLength(), maskTree.random().maxLength())));
                }
                else if (maskTree.choose() != null) {
                    Set<String> from = maskTree.choose().stream().map(x -> (String) x).collect(Collectors.toSet());
                    return Functional.asFunction(Jackson.convertString(new ChooseStringSupplier(new Random(), from)));
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
                if (maskTree.value() != null) {
                    return Functional.asFunction(Jackson.convertInt(new ConstantIntSupplier(maskTree.value().intValue())));
                }
                else if (maskTree.random() != null) {
                    return Functional.asFunction(Jackson.convertInt(new RandomIntSupplier(new Random(), maskTree.random().min(), maskTree.random().max())));
                }
                else if (maskTree.choose() != null) {
                    Set<Integer> from = maskTree.choose().stream().map(x -> (Integer) x).collect(Collectors.toSet());
                    return Functional.asFunction(Jackson.convertInt(new ChooseIntSupplier(new Random(), from)));
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
                if (maskTree.properties() != null) {
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
        // throw new IllegalArgumentException("Invalid mask type: " + maskTree);
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
