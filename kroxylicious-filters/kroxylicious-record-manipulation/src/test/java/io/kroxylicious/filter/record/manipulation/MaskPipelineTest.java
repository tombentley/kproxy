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

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.kroxylicious.filter.record.manipulation.common.Pipeline;
import io.kroxylicious.filter.record.manipulation.common.Strings;
import io.kroxylicious.filter.record.manipulation.config.MaskConfig;
import io.kroxylicious.filter.record.manipulation.jackson.JacksonDeserializer;
import io.kroxylicious.filter.record.manipulation.jackson.JacksonFunction;
import io.kroxylicious.filter.record.manipulation.jackson.JacksonSerializer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Composes the same deserialize/mask/serialize stages as {@link Use}'s demo into {@link Pipeline}s and
 * asserts on the result, rather than just logging it.
 */
class MaskPipelineTest {

    private static final YAMLMapper MAPPER = new YAMLMapper();

    private static final String DATA = """
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

    private static final String MASK_CONTENT = """
            type: object
            properties:
              firstName:
                type: string
                value: "REDACTED"
              aliases:
                type: array
                items:
                  type: string
                  random:
                    minLength: 3
                    maxLength: 15
                    alphabet: abcdefghijklmnopqrstuvwxyz
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

    /** A fixed seed, chosen arbitrarily, that pins every "random"/"choose" mask and every encryption IV drawn below. */
    private static final long SEED = 42L;

    /** Matches the (currently hard-coded) key {@link JacksonFunction} uses for hmac/encrypt/decrypt. */
    private static final byte[] KEY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };

    private final Function<ByteBuffer, JsonNode> deserializer = new JacksonDeserializer(MAPPER);
    private final Function<JsonNode, ByteBuffer> serializer = new JacksonSerializer(MAPPER);

    private JsonNode deserializeResult(ByteBuffer buffer) {
        return deserializer.apply(buffer.duplicate());
    }

    private ByteBuffer mask(MaskConfig maskTree, Random random) {
        Pipeline pipeline = new Pipeline(List.of(deserializer, JacksonFunction.buildMask(maskTree, random), serializer));
        return pipeline.apply(ByteBuffer.wrap(DATA.getBytes(StandardCharsets.UTF_8)));
    }

    private static String hmacOf(String plaintext) {
        return new Strings(KEY, new Random()).hmac().apply(plaintext);
    }

    @Test
    void pipelineDeserializesMasksAndReserializesARecordDeterministically() throws JsonProcessingException {
        // Given
        MaskConfig maskTree = MAPPER.readValue(MASK_CONTENT, MaskConfig.class);

        // When
        JsonNode masked = deserializeResult(mask(maskTree, new Random(SEED)));

        // Then
        assertThat(masked.get("firstName").asText()).isEqualTo("REDACTED");
        assertThat(masked.get("surname").asText()).isEqualTo("Potter");
        assertThat(masked.get("ageYears").asInt()).isEqualTo(52);
        assertThat(masked.get("aliases").valueStream().map(JsonNode::asText).toList()).containsExactly("hwmar", "qdpaaigu");
        assertThat(masked.get("address").get("streetAddress").asText()).isEqualTo(hmacOf("Hogwarts"));
        // The exact ciphertext, captured with SEED fixed: possible only because the IV now comes from a controllable PRNG.
        assertThat(masked.get("address").get("city").asText()).isEqualTo("hS7pcscloO/0SqF0FttWBWFhp3gRALJ2VFcYww9AbMg=");
    }

    @Test
    void maskingWithTheSameSeedIsRepeatable() throws JsonProcessingException {
        // Given
        MaskConfig maskTree = MAPPER.readValue(MASK_CONTENT, MaskConfig.class);

        // When
        JsonNode first = deserializeResult(mask(maskTree, new Random(SEED)));
        JsonNode second = deserializeResult(mask(maskTree, new Random(SEED)));

        // Then
        assertThat(second).isEqualTo(first);
    }

    @Test
    void maskingWithADifferentSeedProducesDifferentRandomValues() throws JsonProcessingException {
        // Given
        MaskConfig maskTree = MAPPER.readValue(MASK_CONTENT, MaskConfig.class);

        // When
        JsonNode first = deserializeResult(mask(maskTree, new Random(SEED)));
        JsonNode second = deserializeResult(mask(maskTree, new Random(SEED + 1)));

        // Then
        assertThat(second.get("ageYears")).isNotEqualTo(first.get("ageYears"));
        assertThat(second.get("aliases")).isNotEqualTo(first.get("aliases"));
    }

    @Test
    void maskThenUnmaskPipelineRoundTripsTheEncryptedFieldButNotTheHmacedField() throws JsonProcessingException {
        // Given
        MaskConfig maskTree = MAPPER.readValue(MASK_CONTENT, MaskConfig.class);
        MaskConfig unmaskTree = MAPPER.readValue(MASK_CONTENT.replace("encrypt", "decrypt"), MaskConfig.class);
        Pipeline unmaskPipeline = new Pipeline(List.of(deserializer, JacksonFunction.buildMask(unmaskTree, new Random(SEED)), serializer));

        // When
        ByteBuffer masked = mask(maskTree, new Random(SEED));
        JsonNode unmaskedTree = deserializeResult(unmaskPipeline.apply(masked));

        // Then
        assertThat(unmaskedTree.get("address").get("city").asText()).isEqualTo("Hogsmead");
        // hmac has no inverse, so the unmask pass re-hmacs the already-masked value rather than recovering "Hogwarts".
        assertThat(unmaskedTree.get("address").get("streetAddress").asText()).isEqualTo(hmacOf(hmacOf("Hogwarts")));
    }

}
