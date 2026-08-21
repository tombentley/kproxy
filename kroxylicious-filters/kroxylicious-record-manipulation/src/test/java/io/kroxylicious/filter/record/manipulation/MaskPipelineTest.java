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

import io.kroxylicious.filter.record.manipulation.common.EncryptStringFunction;
import io.kroxylicious.filter.record.manipulation.common.HmacStringFunction;
import io.kroxylicious.filter.record.manipulation.common.Pipeline;
import io.kroxylicious.filter.record.manipulation.config.SchemaConfig;
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
                apply:
                  - value: "REDACTED"
              aliases:
                type: array
                items:
                  type: string
                  apply:
                    - random:
                        minLength: 3
                        maxLength: 15
                        alphabet: abcdefghijklmnopqrstuvwxyz
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

    private static final String ENCRYPT_THEN_HMAC_CITY = """
            type: object
            properties:
              address:
                type: object
                properties:
                  city:
                    type: string
                    apply:
                      - encrypt:
                          keyId: FOO
                      - hmac:
                          keyId: FOO
            """;

    private static final String HMAC_THEN_ENCRYPT_CITY = """
            type: object
            properties:
              address:
                type: object
                properties:
                  city:
                    type: string
                    apply:
                      - hmac:
                          keyId: FOO
                      - encrypt:
                          keyId: FOO
            """;

    private static final String SCHEMA_WITH_UNRECOGNISED_KEYWORD = """
            type: object
            properties:
              firstName:
                type: string
                pattern: "^[A-Z]"
                apply:
                  - value: "REDACTED"
            """;

    private static final String DELETE_AND_INSERT_CONTENT = """
            type: object
            properties:
              surname:
                type: string
                apply:
                  - delete: true
              nickname:
                type: string
                apply:
                  - value: "Wizard"
            """;

    private static final String INSERT_NESTED_OBJECT_CONTENT = """
            type: object
            properties:
              guardian:
                type: object
                properties:
                  name:
                    type: string
                    apply:
                      - value: "Dumbledore"
            """;

    private static final String DOES_NOT_INSERT_NESTED_OBJECT_CONTENT = """
            type: object
            properties:
              guardian:
                type: object
                properties:
                  name:
                    type: string
                    apply:
                      - hmac:
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

    private ByteBuffer mask(SchemaConfig maskTree, Random random) {
        Pipeline pipeline = new Pipeline(List.of(deserializer, JacksonFunction.buildMask(maskTree, random), serializer));
        return pipeline.apply(ByteBuffer.wrap(DATA.getBytes(StandardCharsets.UTF_8)));
    }

    private static String hmacOf(String plaintext) {
        return new HmacStringFunction(KEY).apply(plaintext);
    }

    @Test
    void pipelineDeserializesMasksAndReserializesARecordDeterministically() throws JsonProcessingException {
        // Given
        SchemaConfig maskTree = MAPPER.readValue(MASK_CONTENT, SchemaConfig.class);

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
        SchemaConfig maskTree = MAPPER.readValue(MASK_CONTENT, SchemaConfig.class);

        // When
        JsonNode first = deserializeResult(mask(maskTree, new Random(SEED)));
        JsonNode second = deserializeResult(mask(maskTree, new Random(SEED)));

        // Then
        assertThat(second).isEqualTo(first);
    }

    @Test
    void maskingWithADifferentSeedProducesDifferentRandomValues() throws JsonProcessingException {
        // Given
        SchemaConfig maskTree = MAPPER.readValue(MASK_CONTENT, SchemaConfig.class);

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
        SchemaConfig maskTree = MAPPER.readValue(MASK_CONTENT, SchemaConfig.class);
        SchemaConfig unmaskTree = MAPPER.readValue(MASK_CONTENT.replace("encrypt", "decrypt"), SchemaConfig.class);
        Pipeline unmaskPipeline = new Pipeline(List.of(deserializer, JacksonFunction.buildMask(unmaskTree, new Random(SEED)), serializer));

        // When
        ByteBuffer masked = mask(maskTree, new Random(SEED));
        JsonNode unmaskedTree = deserializeResult(unmaskPipeline.apply(masked));

        // Then
        assertThat(unmaskedTree.get("address").get("city").asText()).isEqualTo("Hogsmead");
        // hmac has no inverse, so the unmask pass re-hmacs the already-masked value rather than recovering "Hogwarts".
        assertThat(unmaskedTree.get("address").get("streetAddress").asText()).isEqualTo(hmacOf(hmacOf("Hogwarts")));
    }

    @Test
    void composedApplyChainAppliesOperationsInDeclaredOrder() throws JsonProcessingException {
        // Given
        SchemaConfig maskTree = MAPPER.readValue(ENCRYPT_THEN_HMAC_CITY, SchemaConfig.class);

        // When
        JsonNode masked = deserializeResult(mask(maskTree, new Random(SEED)));

        // Then
        String encryptedFirst = new EncryptStringFunction(KEY, new Random(SEED)).apply("Hogsmead");
        assertThat(masked.get("address").get("city").asText()).isEqualTo(hmacOf(encryptedFirst));
    }

    @Test
    void composedApplyChainIsOrderSensitive() throws JsonProcessingException {
        // Given
        SchemaConfig encryptThenHmac = MAPPER.readValue(ENCRYPT_THEN_HMAC_CITY, SchemaConfig.class);
        SchemaConfig hmacThenEncrypt = MAPPER.readValue(HMAC_THEN_ENCRYPT_CITY, SchemaConfig.class);

        // When
        JsonNode encryptFirstResult = deserializeResult(mask(encryptThenHmac, new Random(SEED)));
        JsonNode hmacFirstResult = deserializeResult(mask(hmacThenEncrypt, new Random(SEED)));

        // Then
        assertThat(encryptFirstResult.get("address").get("city").asText())
                .isNotEqualTo(hmacFirstResult.get("address").get("city").asText());
    }

    @Test
    void maskingWithTheSameSeedIsRepeatableForAComposedApplyChain() throws JsonProcessingException {
        // Given
        SchemaConfig maskTree = MAPPER.readValue(ENCRYPT_THEN_HMAC_CITY, SchemaConfig.class);

        // When
        JsonNode first = deserializeResult(mask(maskTree, new Random(SEED)));
        JsonNode second = deserializeResult(mask(maskTree, new Random(SEED)));

        // Then
        assertThat(second).isEqualTo(first);
    }

    @Test
    void schemaWithUnrecognisedJsonSchemaKeywordStillParsesAndMasks() throws JsonProcessingException {
        // Given
        SchemaConfig maskTree = MAPPER.readValue(SCHEMA_WITH_UNRECOGNISED_KEYWORD, SchemaConfig.class);

        // When
        JsonNode masked = deserializeResult(mask(maskTree, new Random(SEED)));

        // Then
        assertThat(masked.get("firstName").asText()).isEqualTo("REDACTED");
    }

    @Test
    void applyDeleteRemovesAnExistingPropertyAndApplyValueInsertsAMissingOne() throws JsonProcessingException {
        // Given
        SchemaConfig maskTree = MAPPER.readValue(DELETE_AND_INSERT_CONTENT, SchemaConfig.class);

        // When
        JsonNode masked = deserializeResult(mask(maskTree, new Random(SEED)));

        // Then
        assertThat(masked.has("surname")).isFalse();
        assertThat(masked.get("nickname").asText()).isEqualTo("Wizard");
    }

    @Test
    void applyValueInsertsANestedObjectThatIsEntirelyAbsentFromTheData() throws JsonProcessingException {
        // Given
        SchemaConfig maskTree = MAPPER.readValue(INSERT_NESTED_OBJECT_CONTENT, SchemaConfig.class);

        // When
        JsonNode masked = deserializeResult(mask(maskTree, new Random(SEED)));

        // Then
        assertThat(masked.get("guardian").get("name").asText()).isEqualTo("Dumbledore");
    }

    @Test
    void applyHmacDoesNotInsertANestedObjectThatIsEntirelyAbsentFromTheData() throws JsonProcessingException {
        // Given
        SchemaConfig maskTree = MAPPER.readValue(DOES_NOT_INSERT_NESTED_OBJECT_CONTENT, SchemaConfig.class);

        // When
        JsonNode masked = deserializeResult(mask(maskTree, new Random(SEED)));

        // Then
        assertThat(masked.has("guardian")).isFalse();
    }

}
