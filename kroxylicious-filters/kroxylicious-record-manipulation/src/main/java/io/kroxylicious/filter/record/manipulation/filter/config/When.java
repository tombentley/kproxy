/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import io.kroxylicious.filter.record.manipulation.filter.Direction;

/**
 * Criteria for when a {@link Manipulation} should apply
 * @param direction Whether the records are headed into, or out of, the cluster
 * @param inTopic A predicate on the record's containing topic
 * @param withKeySchema A predicate on the data in the record
 */
public record When(
                   Direction direction,
                   TopicName inTopic,
                   SchemaRef withKeySchema,
                   SchemaRef withValueSchema,
                   HeaderPredicate hasAnyHeader) {}
