/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.mapper.json;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.InvalidJsonPatchException;

import io.kroxylicious.filter.transformation.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.mapper.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApplyJsonPatchTest {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final ApplyJsonPatch ADD_ONE;
    static {
        try {
            ADD_ONE = new ApplyJsonPatch(
                    OBJECT_MAPPER.readTree("""
                            [
                            {"op":"add", "path":"/one", "value":1}
                            ]"""));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
        assertThat(ADD_ONE.acceptedType()).isEqualTo(JsonNode.class);
    }

    @Test
    void shouldHaveJsonNodeReturnType() {
        assertThat(ADD_ONE.returnedType()).isEqualTo(JsonNode.class);
    }

    @Test
    void shouldApply() {
        // Given
        ObjectNode node = OBJECT_MAPPER.getNodeFactory().objectNode();

        // When
        var transformedNode = ADD_ONE.transform(node, new Context("", List.of(), RecordDataLocation.KeyDataLocation.INSTANCE));

        // Then
        assertThat(transformedNode.isObject()).isTrue();
        assertThat(transformedNode.size()).isOne();
        assertThat(((ObjectNode) transformedNode).get("one").asInt()).isEqualTo(1);
    }

}