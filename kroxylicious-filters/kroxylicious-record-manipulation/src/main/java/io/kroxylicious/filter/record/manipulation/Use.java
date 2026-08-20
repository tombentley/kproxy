/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.kroxylicious.filter.record.manipulation.common.Pipeline;
import io.kroxylicious.filter.record.manipulation.config.MaskConfig;
import io.kroxylicious.filter.record.manipulation.jackson.JacksonDeserializer;
import io.kroxylicious.filter.record.manipulation.jackson.JacksonFunction;
import io.kroxylicious.filter.record.manipulation.jackson.JacksonSerializer;
import io.kroxylicious.filter.record.manipulation.jackson.JacksonSupplier;

/**
 * A demo of building a JSON mask/generator {@link Function} or {@link Supplier} from a {@link MaskConfig} tree.
 */
public class Use {

    private static final Logger LOGGER = LoggerFactory.getLogger(Use.class);
    private static final YAMLMapper MAPPER = new YAMLMapper();

    private Use() {
    }

    /**
     * Runs the demo.
     * @param args unused
     * @throws JsonProcessingException if the demo YAML content cannot be parsed
     */
    public static void main(String[] args) throws JsonProcessingException {
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

        Function<ByteBuffer, JsonNode> deserializer = new JacksonDeserializer(MAPPER);

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

        Function<JsonNode, ByteBuffer> serializer = new JacksonSerializer(MAPPER);

        JacksonFunction maskFn = JacksonFunction.buildMask(maskTree);
        Pipeline maskPipeline = new Pipeline(List.of(deserializer, maskFn, serializer));
        ByteBuffer result = maskPipeline.apply(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));
        String masked = StandardCharsets.UTF_8.decode(result.duplicate()).toString();
        LOGGER.atInfo().addKeyValue("masked", masked).log("applied mask");

        JacksonFunction unmaskFn = JacksonFunction.buildMask(unmaskTree);
        Pipeline unmaskPipeline = new Pipeline(List.of(deserializer, unmaskFn, serializer));
        ByteBuffer result2 = unmaskPipeline.apply(result);
        String unmasked = StandardCharsets.UTF_8.decode(result2.duplicate()).toString();
        LOGGER.atInfo().addKeyValue("unmasked", unmasked).log("applied unmask");

        JacksonSupplier supplierFn = JacksonSupplier.buildGenerator(maskTree);
        var generatedResult = supplierFn.get();
        String generated = MAPPER.writeValueAsString(generatedResult);
        LOGGER.atInfo().addKeyValue("generated", generated).log("generated data");

    }

}
