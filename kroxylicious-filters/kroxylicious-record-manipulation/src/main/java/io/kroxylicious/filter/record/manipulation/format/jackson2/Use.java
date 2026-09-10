/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson2;

import java.nio.ByteBuffer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.ServiceLoaderPluginLookup;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A demo of building a JSON mask/generator {@link JacksonFunction} from a {@link SchemaConfig} tree.
 */
@SuppressFBWarnings(value = "HARD_CODE_KEY", justification = "Use is a main()-based demo, not production wiring - this module has no Filter "
        + "integration yet (see module README), so there's no real key-management path to source key material from. The literal here is an "
        + "illustrative placeholder standing in for a Context built from a real key at call sites that do exist.")
public class Use {

    private static final Logger LOGGER = LoggerFactory.getLogger(Use.class);
    private static final YAMLMapper MAPPER = new YAMLMapper();
    private static final byte[] KEY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };
    private static final PluginLookup LOOKUP = new ServiceLoaderPluginLookup();

    private Use() {
    }

    /**
     * Runs the demo.
     * @param args unused
     * @throws JsonProcessingException if the demo YAML content cannot be parsed
     */
    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "The PRNG is deliberately injected rather than SecureRandom, "
            + "so that masking can eventually be made to have repeatable-read semantics (e.g. seeded from topic/partition/offset) - "
            + "see EncryptStringFunction, which carries the same justification for the same reason.")
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
                    apply:
                      - op: ValueString
                        value: "REDACTED"
                  #surname:
                  #  type: string
                  #  apply:
                  #    - op: ChooseString
                  #      from:
                  #        - Smith
                  #        - Jones
                  aliases:
                    type: array
                    items:
                      type: string
                      apply:
                        - op: RandomString
                          minLengthInclusive: 3
                          maxLengthExclusive: 15
                          alphabet: abcdef ghijklmnopqrst uvwxyz
                  ageYears:
                    type: integer
                    apply:
                      - op: RandomInt
                        minInclusive: 18
                        maxExclusive: 100
                  address:
                    type: object
                    properties:
                      streetAddress:
                        type: string
                        apply:
                          - op: HmacString
                            keyId: FOO
                      city:
                        type: string
                        apply:
                          - op: EncryptString
                            keyId: FOO
                """;
        // The above assumes that every node has a singular `type`.
        // That's fine so long as things like `random` work with multiple types
        SchemaConfig maskTree = MAPPER.readValue(maskContent, SchemaConfig.class);
        SchemaConfig unmaskTree = MAPPER.readValue(maskContent.replace("EncryptString", "DecryptString"), SchemaConfig.class);

        // Function<JsonNode, ByteBuffer> serializer = new JacksonSerializer(MAPPER);
        //
        // OpContext maskOpContext = new OpContext(new Random(), KEY);
        // TypedOp<JsonNode, JsonNode> maskFn = JacksonFunction.buildMask(maskTree, LOOKUP);
        // Pipeline<ByteBuffer, Void, ByteBuffer> maskPipeline = new Pipeline<>(List.of(deserializer, maskFn.bind(maskOpContext), serializer));
        // ByteBuffer result = Functional.bind(maskPipeline, null).apply(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));
        // String masked = StandardCharsets.UTF_8.decode(result.duplicate()).toString();
        // LOGGER.atInfo().addKeyValue("masked", masked).log("applied mask");
        //
        // OpContext unmaskOpContext = new OpContext(new Random(), KEY);
        // TypedOp<JsonNode, JsonNode> unmaskFn = JacksonFunction.buildMask(unmaskTree, LOOKUP);
        // Pipeline<ByteBuffer, Void, ByteBuffer> unmaskPipeline = new Pipeline<>(List.of(deserializer, unmaskFn.bind(unmaskOpContext), serializer));
        // ByteBuffer result2 = Functional.bind(unmaskPipeline, null).apply(result);
        // String unmasked = StandardCharsets.UTF_8.decode(result2.duplicate()).toString();
        // LOGGER.atInfo().addKeyValue("unmasked", unmasked).log("applied unmask");
        //
        // // Root-level generation is just this same traversal, started from MissingNode instead of a real value.
        // OpContext generateOpContext = new OpContext(new Random(), KEY);
        // JsonNode generatedResult = JacksonFunction.buildMask(maskTree, LOOKUP).apply(MissingNode.getInstance(), generateOpContext);
        // String generated = MAPPER.writeValueAsString(generatedResult);
        // LOGGER.atInfo().addKeyValue("generated", generated).log("generated data");

    }

}
