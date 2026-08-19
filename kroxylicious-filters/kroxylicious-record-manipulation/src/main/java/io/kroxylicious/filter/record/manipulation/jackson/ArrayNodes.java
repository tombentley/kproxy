/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * https://json-schema.org/understanding-json-schema/reference/array
 * items
 * TODO prefixItems
 * TODO additionalItems and unevaluatedItems
 * TODO minItems and maxItems (for generation)
 * Not supported: contains, minContains, maxContains
 */
public class ArrayNodes {

    private final JsonNodeFactory nodeFactory;

    public ArrayNodes(JsonNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public Function<ArrayNode, JsonNode> items(Function<? super JsonNode, ? extends JsonNode> itemsFn) {
        return arrayNode -> {
            ArrayNode result = arrayNode.arrayNode(arrayNode.size());
            arrayNode.valueStream().map(itemsFn).forEach(result::add);
            return result;
        };
    }

    public Supplier<JsonNode> items2(Supplier<? extends JsonNode> itemsFn) {
        return () -> {
            int length = 2; // TODO a random length
            ArrayNode result = nodeFactory.arrayNode(length);
            for (int i = 0; i < length; i++) {
                result.add(itemsFn.get());
            }
            return result;
        };
    }

    /*


    when: // list (OR) of records (AND)
    - topicName:
        equals: foo
      subject:
        contains:
          type: User
          name: Bob
      clientId:
        matches: foo.*
      keySchemaId:
        equals: 123
        location: prefix
    operations:
      - signatureValidation:
      - schemaValidation:
      - manipulation:  // e.g. field encryption because we can encrypt multiple times if needed we can assume encryptions only need one key
      - manipulation:  // e.g. masking/redaction
      - encryption:
      - compression:

      onException (for produce vs for consume) -> DLQ, Reject, replace with empty


     */
}
