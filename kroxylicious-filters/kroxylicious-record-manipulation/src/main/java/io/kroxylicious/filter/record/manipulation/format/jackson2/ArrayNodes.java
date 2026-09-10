/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;

/**
 * https://json-schema.org/understanding-json-schema/reference/array
 * items
 * TODO prefixItems
 * TODO additionalItems and unevaluatedItems
 * TODO minItems and maxItems (for generation)
 * Not supported: contains, minContains, maxContains
 */
public class ArrayNodes {

    private ArrayNodes() {
    }

    /**
     * Maps the elements of an array.
     * @param itemsFn the function applied to each element of the array
     * @return a function mapping an {@link ArrayNode} to a new array with {@code itemsFn} applied to each element
     */
    public static BaseTypedOp<ArrayNode, JsonNode> items(BaseTypedOp<JsonNode, JsonNode> itemsFn) {
        return BaseTypedOp.of(ArrayNode.class, JsonNode.class, (arrayNode, context) -> new ArrayItems().modifyAll(arrayNode, itemsFn, context));
    }

    /*
     *
     *
     * when: // list (OR) of records (AND)
     * - topicName:
     * equals: foo
     * subject:
     * contains:
     * type: User
     * name: Bob
     * clientId:
     * matches: foo.*
     * keySchemaId:
     * equals: 123
     * location: prefix
     * operations:
     * - signatureValidation:
     * - schemaValidation:
     * - manipulation: // e.g. field encryption because we can encrypt multiple times if needed we can assume encryptions only need one key
     * - manipulation: // e.g. masking/redaction
     * - encryption:
     * - compression:
     *
     * onException (for produce vs for consume) -> DLQ, Reject, replace with empty
     *
     *
     */
}
