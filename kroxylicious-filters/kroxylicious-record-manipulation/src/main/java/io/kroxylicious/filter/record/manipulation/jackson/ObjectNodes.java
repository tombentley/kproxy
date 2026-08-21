/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
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
     * Maps selected properties of an object, and can also delete or insert properties.
     * <p>
     * Builds a fresh object (the input is never mutated): each property present in the input is passed
     * through its mapped function if one exists in {@code map} (or carried over unchanged otherwise); each
     * key present in {@code map} but absent from the input is additionally invoked, with
     * {@link MissingNode#getInstance()} as its input, to support inserting a property that didn't
     * previously exist. In both cases, a function result that {@link JsonNode#isMissingNode()} is omitted
     * from the result rather than written back - this is how a function signals "delete this property" (if
     * it was present) or "decline to insert" (if it wasn't).
     * <p>
     * <b>Contract:</b> functions passed here must treat a {@code MissingNode} input as "there is no prior
     * value" and return {@code MissingNode} themselves if they decline to produce a value from nothing -
     * e.g. an operation that requires transforming an existing value, rather than generating a fresh one.
     * A function that doesn't account for this (e.g. one that just does {@code node.asInt() + 1}) will
     * silently misbehave when invoked on an absent property (Jackson's {@code MissingNode.asInt()}
     * defaults to {@code 0}) rather than erroring.
     * @param map the per-property functions, keyed by property name
     * @return a function building a fresh object per the rules above
     */
    public Function<ObjectNode, ObjectNode> mapProperties(Map<String, ? extends Function<? super JsonNode, ? extends JsonNode>> map) {
        return new JsonNodePropertiesFunction(nodeFactory, map);
    }

    /**
     * Generates an object from a set of per-property suppliers.
     * @param map the per-property suppliers, keyed by property name
     * @return a supplier that builds a new object with one property per entry in {@code map}, populated by calling the supplier
     */
    public Supplier<ObjectNode> mapProperties2(Map<String, ? extends Supplier<? extends JsonNode>> map) {
        return new JsonNodePropertiesSupplier(nodeFactory, map);
    }

    private record JsonNodePropertiesFunction(JsonNodeFactory nodeFactory,
                                              Map<String, ? extends Function<? super JsonNode, ? extends JsonNode>> propertyFns)
            implements Function<ObjectNode, ObjectNode> {

        @Override
        public ObjectNode apply(ObjectNode object) {
            ObjectNode result = nodeFactory.objectNode();
            Set<String> handled = new HashSet<>();
            for (var property : object.properties()) {
                handled.add(property.getKey());
                var mapFn = propertyFns.get(property.getKey());
                JsonNode mapped = mapFn != null ? mapFn.apply(property.getValue()) : property.getValue();
                if (!mapped.isMissingNode()) {
                    result.set(property.getKey(), mapped);
                }
            }
            for (var entry : propertyFns.entrySet()) {
                if (handled.contains(entry.getKey())) {
                    continue;
                }
                JsonNode mapped = entry.getValue().apply(MissingNode.getInstance());
                if (!mapped.isMissingNode()) {
                    result.set(entry.getKey(), mapped);
                }
            }
            return result;
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
