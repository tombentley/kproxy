/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.InvalidJsonPatchException;

import io.kroxylicious.filter.transformation.Datum;
import io.kroxylicious.filter.transformation.GlobalId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplyJsonPatchTest {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private ApplyJsonPatch patcher;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        patcher = new ApplyJsonPatch(
                OBJECT_MAPPER.readTree("""
                        [
                        {"op":"add", "path":"/one", "value":1}
                        ]"""));
    }

    @Test
    void shouldRejectInvalidPatch() throws JsonProcessingException {
        JsonNode invalidPatch = OBJECT_MAPPER.readTree("""
                [
                {"op":"add", "path":"/one" }
                ]""");
        assertThatThrownBy(() -> new ApplyJsonPatch(invalidPatch))
                .isExactlyInstanceOf(InvalidJsonPatchException.class)
                .hasMessage("Invalid JSON Patch payload (missing 'value' field)");
    }

    @Test
    void shouldHaveJsonNodeAccetpedType() {
        assertThat(patcher.acceptedType()).isEqualTo(JsonNode.class);
    }

    @Test
    void shouldHaveJsonNodeReturnType() {
        assertThat(patcher.returnedType()).isEqualTo(JsonNode.class);
    }

    @Test
    void shouldApply() throws JsonProcessingException {
        // Given
        var datum = new Datum<>(new GlobalId(1), JsonNode.class, OBJECT_MAPPER.getNodeFactory().objectNode());

        // When
        var transformedNode = patcher.transform(datum.datum());

        // Then
        assertThat(transformedNode.isObject()).isTrue();
        assertThat(transformedNode.size()).isOne();
        assertThat(((ObjectNode) transformedNode).get("one").asInt()).isEqualTo(1);
    }

}