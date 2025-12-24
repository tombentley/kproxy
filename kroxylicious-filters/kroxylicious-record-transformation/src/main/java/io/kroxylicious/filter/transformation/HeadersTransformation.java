/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.util.List;

import org.apache.kafka.common.header.Header;

public interface HeadersTransformation {
    HeadersTransformation IDENTITY = headers -> headers;
    HeadersTransformation EMPTY = headers -> List.of();

    static HeadersTransformation headers() {
        return IDENTITY;
    }

    static HeadersTransformation empty() {
        return EMPTY;
    }

    List<Header> transformHeaders(List<Header> headers);
}

