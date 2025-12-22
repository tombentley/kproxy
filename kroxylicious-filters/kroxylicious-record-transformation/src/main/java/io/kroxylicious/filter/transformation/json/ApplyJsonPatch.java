/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.json;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.JsonPatch;

import io.kroxylicious.filter.transformation.Datum;
import io.kroxylicious.filter.transformation.DatumMapper;
import io.kroxylicious.filter.transformation.SchemaIdentifier;
import io.kroxylicious.filter.transformation.SchemaTransformation;

/**
 * Apply a given <a href="https://datatracker.ietf.org/doc/html/rfc6902">RFC-6902 JSON Patch</a> to buffers containing data in JSON format.
 */
public class ApplyJsonPatch implements DatumMapper<JsonNode, JsonNode> {

    private final JsonNode patch;
    private final SchemaTransformation schemaTransformation;

    public ApplyJsonPatch(JsonNode patch,
                          SchemaTransformation schemaTransformation) {
        this.patch = Objects.requireNonNull(patch);
        this.schemaTransformation = schemaTransformation;
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
    public Datum<JsonNode> transform(Datum<JsonNode> datum) {
        JsonNode target = JsonPatch.apply(patch, datum.datum());
        SchemaIdentifier schemaIdentifier = schemaTransformation.schemaIdentifier(datum.schemaIdentifier());
        return new Datum<>(schemaIdentifier, JsonNode.class, target);

    }
}
