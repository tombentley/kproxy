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

    /**
     * Creates an instance.
     * @param nodeFactory the factory used to build result nodes
     */
    public ObjectNodes(JsonNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    /**
     * Maps selected properties of an object.
     * @param map the per-property functions, keyed by property name
     * @return a function that replaces each property present in {@code map} with the result of applying its function,
     *         leaving other properties unchanged
     */
    public Function<ObjectNode, ObjectNode> mapProperties(Map<String, ? extends Function<? super JsonNode, ? extends JsonNode>> map) {
        return new JsonNodePropertiesFunction(map);
    }

    /**
     * Generates an object from a set of per-property suppliers.
     * @param map the per-property suppliers, keyed by property name
     * @return a supplier that builds a new object with one property per entry in {@code map}, populated by calling the supplier
     */
    public Supplier<ObjectNode> mapProperties2(Map<String, ? extends Supplier<? extends JsonNode>> map) {
        return new JsonNodePropertiesSupplier(nodeFactory, map);
    }

    private record JsonNodePropertiesFunction(Map<String, ? extends Function<? super JsonNode, ? extends JsonNode>> propertyFns)
            implements Function<ObjectNode, ObjectNode> {

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
                                              Map<String, ? extends Supplier<? extends JsonNode>> map)
            implements Supplier<ObjectNode> {
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
