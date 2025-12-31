/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.api.RecordDataLocation;

/**
 * Thr context of a particular transformation
 * @param topicName The name of the topic which contains the record
 * @param headers The record headers
 * @param location The location
 */
public record Context(String topicName,
                      List<Header> headers,
                      RecordDataLocation location) {
}
