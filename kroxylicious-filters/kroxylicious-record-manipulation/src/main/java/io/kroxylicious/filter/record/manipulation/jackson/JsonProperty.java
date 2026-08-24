/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.kroxylicious.filter.record.manipulation.common.Maybe;
import io.kroxylicious.filter.record.manipulation.common.Property;

/**
 * A {@link Property} focusing on one named property of an {@link ObjectNode}.
 * @param key the property name
 */
public record JsonProperty(String key) implements Property<ObjectNode, JsonNode> {

    @Override
    public Maybe<JsonNode> get(ObjectNode object) {
        return object.has(key) ? Maybe.some(object.get(key)) : Maybe.none();
    }

    @Override
    public ObjectNode set(ObjectNode object, JsonNode value) {
        // A shallow copy suffices - and is far cheaper than object.deepCopy() - because only the top-level
        // property map needs to be independent of the input; nested values are never mutated in place, only
        // ever replaced wholesale, so sharing their references with the input is safe.
        ObjectNode result = object.objectNode().setAll(object);
        result.set(key, value);
        return result;
    }

    @Override
    public ObjectNode clear(ObjectNode object) {
        ObjectNode result = object.objectNode().setAll(object);
        result.remove(key);
        return result;
    }
}
