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

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.kroxylicious.filter.record.manipulation.common.Pipeline;
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

    private final Function<ByteBuffer, JsonNode> deserializer = new JacksonDeserializer(MAPPER);
    private final Function<JsonNode, ByteBuffer> serializer = new JacksonSerializer(MAPPER);

    private JsonNode deserializeResult(ByteBuffer buffer) {
        return deserializer.apply(buffer.duplicate());
    }

    @Test
    void pipelineDeserializesMasksAndReserializesARecord() throws JsonProcessingException {
        // Given
        MaskConfig maskTree = MAPPER.readValue(MASK_CONTENT, MaskConfig.class);
        Pipeline pipeline = new Pipeline(List.of(deserializer, JacksonFunction.buildMask(maskTree), serializer));

        // When
        ByteBuffer result = pipeline.apply(ByteBuffer.wrap(DATA.getBytes(StandardCharsets.UTF_8)));

        // Then
        JsonNode masked = deserializeResult(result);
        assertThat(masked.get("firstName").asText()).isEqualTo("REDACTED");
        assertThat(masked.get("surname").asText()).isEqualTo("Potter");
        assertThat(masked.get("ageYears").asInt()).isBetween(18, 100);
        assertThat(masked.get("aliases")).hasSize(2);
        assertThat(masked.get("address").get("streetAddress").asText()).isNotEqualTo("Hogwarts");
        assertThat(masked.get("address").get("city").asText()).isNotEqualTo("Hogsmead");
    }

    @Test
    void maskThenUnmaskPipelineRoundTripsTheEncryptedFieldButNotTheHmacedField() throws JsonProcessingException {
        // Given
        MaskConfig maskTree = MAPPER.readValue(MASK_CONTENT, MaskConfig.class);
        MaskConfig unmaskTree = MAPPER.readValue(MASK_CONTENT.replace("encrypt", "decrypt"), MaskConfig.class);
        Pipeline maskPipeline = new Pipeline(List.of(deserializer, JacksonFunction.buildMask(maskTree), serializer));
        Pipeline unmaskPipeline = new Pipeline(List.of(deserializer, JacksonFunction.buildMask(unmaskTree), serializer));

        // When
        ByteBuffer masked = maskPipeline.apply(ByteBuffer.wrap(DATA.getBytes(StandardCharsets.UTF_8)));
        ByteBuffer unmasked = unmaskPipeline.apply(masked);

        // Then
        JsonNode unmaskedTree = deserializeResult(unmasked);
        assertThat(unmaskedTree.get("address").get("city").asText()).isEqualTo("Hogsmead");
        assertThat(unmaskedTree.get("address").get("streetAddress").asText()).isNotEqualTo("Hogwarts");
    }

}
