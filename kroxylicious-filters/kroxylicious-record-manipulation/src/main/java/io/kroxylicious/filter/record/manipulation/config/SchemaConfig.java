/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * A JSON-Schema-shaped node describing where {@code apply} chains attach.
 * <p>
 * Only {@code type}, {@code properties}, {@code items}, and the non-standard {@code apply} keyword are given
 * explicit meaning here. Every other real JSON Schema keyword (e.g. {@code pattern}, {@code contains},
 * {@code minLength}, {@code enum}, {@code patternProperties}) is accepted and preserved via
 * {@link #otherKeywords()} but not interpreted, so an existing JSON Schema document can have {@code apply}
 * added to it directly rather than needing its unrelated keywords stripped out first.
 * @param type the JSON type this schema describes, e.g. {@code object}, {@code array}, {@code string}, {@code integer}
 * @param properties for {@code type: object}, the schema for each named property
 * @param items for {@code type: array}, the schema applied to each element
 * @param apply the sequence of operations to run on this node's value, applied after any nested
 *              {@code properties}/{@code items} have already been applied to it
 * @param otherKeywords any other JSON Schema keyword present on this node; preserved but not interpreted,
 *                       and re-emitted at this node's top level (not nested) if this config is serialized
 */
public record SchemaConfig(String type,
                           Map<String, SchemaConfig> properties,
                           SchemaConfig items,
                           List<ApplyConfig> apply,
                           @JsonAnySetter @JsonAnyGetter Map<String, Object> otherKeywords) {}
