/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * <pre>{@code
 * config:
 *   schemaRegistry:
 *     url:
 *     tls:
 *   manipulations:
 *     - when:
 *         direction: INTO_CLUSTER
 *         inTopic:   # default is to match all topics
 *           withName: "foo"
 *           # OR
 *           withNameIn: ["bar", "baz"]
 *           # OR
 *           withNameMatching: "quux-.*"
 *         withKeySchema: ## this is a method for selecting records =/=> we should fetch the content (that would depend on the needs of the `then` clause)
 *           groupId: com.myco.billing
 *           artifactId: orders-value
 *           version: *  # default is to match all
 *         withValueSchema:
 *           ...
 *         hasAnyHeader:
 *           withKey: my-key
 *         clientSubject:
 *           containsPrincipal:
 *             withType: User
 *             withName: or withNameIn or withNameMatching
 *       then:
 *         newKey:
 *           transforms:
 *             - transform: ReadJson
 *               config:
 *             - transform: JsonSchemaValidation
 *               config:
 *             - transform: Jsonata
 *               jsonata: |
 *             - transform: WriteJson
 *               config:
 *         newValue:
 *           reader: JsonRead # only needed
 *           readerConfig:
 *           transforms:
 *             - transform: Jsonata
 *               jsonata: |
 *           writer: WriteJson
 *           writerConfig:
 * }</pre>
 */
public record RecordManipulationConfig(
                                       @Nullable SchemaRegistry schemaRegistry,
                                       @JsonProperty(required = true) List<Manipulation> manipulations) {
    public RecordManipulationConfig {
        if (schemaRegistry == null &&
                manipulations.stream().anyMatch(manipulation -> manipulation.when().withValueSchema() != null || manipulation.when().withKeySchema() != null)) {
            throw new IllegalArgumentException("schemaRegistry is required when a manipulation has a when.withValueSchema or when.withKeySchema");
        }
    }
}
