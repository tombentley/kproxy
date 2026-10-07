/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.service;

import java.util.stream.Stream;

import io.kroxylicious.kafka.common.header.Header;

public interface HeadersTransform {
    Stream<Header> transform(Stream<Header> headers);
}
