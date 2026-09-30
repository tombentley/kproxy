/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import java.util.List;

/**
 * config:
 *   schemaRegistry:
 *     url:
 *     tls:
 *   manipulations:
 *     - when:
 *         direction: TO_BROKER
 *         inTopic:   # default is to match all topics
 *           withNameMatching: .*
 *           withName
 *           withNameIn
 *         withKeySchema: ## this is a method for selecting records =/=> we should fetch the content (that would depend on the needs of the `then` clause)
 *           groupId: com.myco.billing
 *           artifactId: orders-value
 *           version: *  # default is to match all
 *         withValueSchema:
 *           ...
 *         hasAnyHeader:
 *           withKey: my-key
 *       then:
 *         newKey:
 *           reader: JsonRead # only needed
 *           readerConfig:
 *           transforms:
 *             - transform: Jsonata
 *               jsonata: |
 *           writer: WriteJson
 *           writerConfig:
 *         newValue:
 *           reader: JsonRead # only needed
 *           readerConfig:
 *           transforms:
 *             - transform: Jsonata
 *               jsonata: |
 *           writer: WriteJson
 *           writerConfig:
 *
 */

public record Foo(
                  SchemaRegistry schemaRegistry,
                  List<Manipulation> manipulations) {}
