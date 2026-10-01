/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Criteria for when a {@link Manipulation} should apply
 * @param direction Whether the records are headed into, or out of, the cluster
 * @param inTopic A predicate on the record's containing topic
 * @param withKeySchema A predicate on the data in the record
 */
public record When(
                   @JsonProperty(required = true) Direction direction,
                   @Nullable TopicName inTopic,
                   @Nullable SchemaRef withKeySchema,
                   @Nullable SchemaRef withValueSchema,
                   @Nullable HeaderPredicate hasAnyHeader) {}
