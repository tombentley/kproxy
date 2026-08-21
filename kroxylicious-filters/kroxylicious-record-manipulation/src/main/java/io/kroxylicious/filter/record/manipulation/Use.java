/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.Pipeline;
import io.kroxylicious.filter.record.manipulation.config.SchemaConfig;
import io.kroxylicious.filter.record.manipulation.jackson.JacksonDeserializer;
import io.kroxylicious.filter.record.manipulation.jackson.JacksonFunction;
import io.kroxylicious.filter.record.manipulation.jackson.JacksonSerializer;

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
                      - value: "REDACTED"
                  #surname:
                  #  type: string
                  #  apply:
                  #    - choose:
                  #        - Smith
                  #        - Jones
                  aliases:
                    type: array
                    items:
                      type: string
                      apply:
                        - random:
                            minLength: 3
                            maxLength: 15
                            alphabet: abcdef ghijklmnopqrst uvwxyz
                  ageYears:
                    type: integer
                    apply:
                      - random:
                          min: 18
                          max: 100
                  address:
                    type: object
                    properties:
                      streetAddress:
                        type: string
                        apply:
                          - hmac:
                              keyId: FOO
                      city:
                        type: string
                        apply:
                          - encrypt:
                              keyId: FOO
                """;
        // The above assumes that every node has a singular `type`.
        // That's fine so long as things like `random` work with multiple types
        SchemaConfig maskTree = MAPPER.readValue(maskContent, SchemaConfig.class);
        SchemaConfig unmaskTree = MAPPER.readValue(maskContent.replace("encrypt", "decrypt"), SchemaConfig.class);

        Function<JsonNode, ByteBuffer> serializer = new JacksonSerializer(MAPPER);

        Context maskContext = new Context(new Random(), KEY);
        JacksonFunction maskFn = JacksonFunction.buildMask(maskTree);
        Pipeline maskPipeline = new Pipeline(List.of(deserializer, maskFn.bind(maskContext), serializer));
        ByteBuffer result = maskPipeline.apply(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));
        String masked = StandardCharsets.UTF_8.decode(result.duplicate()).toString();
        LOGGER.atInfo().addKeyValue("masked", masked).log("applied mask");

        Context unmaskContext = new Context(new Random(), KEY);
        JacksonFunction unmaskFn = JacksonFunction.buildMask(unmaskTree);
        Pipeline unmaskPipeline = new Pipeline(List.of(deserializer, unmaskFn.bind(unmaskContext), serializer));
        ByteBuffer result2 = unmaskPipeline.apply(result);
        String unmasked = StandardCharsets.UTF_8.decode(result2.duplicate()).toString();
        LOGGER.atInfo().addKeyValue("unmasked", unmasked).log("applied unmask");

        // Root-level generation is just this same traversal, started from MissingNode instead of a real value.
        Context generateContext = new Context(new Random(), KEY);
        JsonNode generatedResult = JacksonFunction.buildMask(maskTree).apply(MissingNode.getInstance(), generateContext);
        String generated = MAPPER.writeValueAsString(generatedResult);
        LOGGER.atInfo().addKeyValue("generated", generated).log("generated data");

    }

}
