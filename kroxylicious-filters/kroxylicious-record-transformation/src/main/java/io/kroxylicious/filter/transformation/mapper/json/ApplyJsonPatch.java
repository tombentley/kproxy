/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.mapper.json;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.JsonPatch;

import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.Mapper;

/**
 * Apply a given <a href="https://datatracker.ietf.org/doc/html/rfc6902">RFC-6902 JSON Patch</a> to buffers containing data in JSON format.
 */
public class ApplyJsonPatch implements Mapper<JsonNode, JsonNode> {

    private final JsonNode patch;

    public ApplyJsonPatch(JsonNode patch) {
        this.patch = Objects.requireNonNull(patch);
        JsonPatch.validate(patch);
    }

    @Override
    public Class<JsonNode> acceptedType() {
        return JsonNode.class;
    }

    @Override
    public Class<JsonNode> returnedType() {
        return JsonNode.class;
    }

    @Override
    public JsonNode transform(JsonNode value, Context context) {
        JsonNode target = JsonPatch.apply(patch, value);
        return target;
    }

}
