/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

public record Path(Identifier identifier, List<Segment> segments, Consumer<JsonNode> consumer) {
    @Override
    public String toString() {
        return identifier.symbol +
                segments.stream().map(Object::toString).collect(Collectors.joining("")) +
                ", consumer=" + consumer +
                '}';
    }
}
