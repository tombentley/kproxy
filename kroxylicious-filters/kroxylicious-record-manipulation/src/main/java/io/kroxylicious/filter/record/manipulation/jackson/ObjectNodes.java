/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * TODO patternProperties (invocation order wrt properties)
 * TODO additionalProperties and unevaluatedPropertoes
 * TODO required (for generation, not for masking)
 * Not supported: propertyNames, minProperties, maxProperties
 * https://json-schema.org/understanding-json-schema/reference/object
 */
public class ObjectNodes {

    private final JsonNodeFactory nodeFactory;

    public ObjectNodes(JsonNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public Function<ObjectNode, ObjectNode> mapProperties(Map<String, Function<? super JsonNode, ? extends JsonNode>> map) {
        return new JsonNodePropertiesFunction(map);
    }

    public Supplier<ObjectNode> mapProperties2(Map<String, Supplier<? extends JsonNode>> map) {
        return new JsonNodePropertiesSupplier(nodeFactory, map);
    }

    private record JsonNodePropertiesFunction(Map<String, Function<? super JsonNode, ? extends JsonNode>> propertyFns) implements Function<ObjectNode, ObjectNode> {

        @Override
        public ObjectNode apply(ObjectNode object) {
            for (var property : object.properties()) {
                var mapFn = propertyFns.get(property.getKey());
                if (mapFn != null) {
                    object.replace(property.getKey(), mapFn.apply(property.getValue()));
                }
            }
            return object;
        }
    }

    private record JsonNodePropertiesSupplier(JsonNodeFactory nodeFactory,
                                              Map<String, Supplier<? extends JsonNode>> map) implements Supplier<ObjectNode> {
        @Override
        public ObjectNode get() {
            var result = nodeFactory.objectNode();
            for (var property : map.entrySet()) {
                var mapFn = property.getValue();
                result.set(property.getKey(), mapFn.get());
            }
            return result;
        }
    }
}
