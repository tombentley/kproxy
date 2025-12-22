/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import org.apache.kafka.common.header.Header;

public interface HeadersTransformation {
    HeadersTransformation IDENTITY = headers -> headers;
    HeadersTransformation EMPTY = headers -> new Header[0];

    static HeadersTransformation headers() {
        return IDENTITY;
    }

    static HeadersTransformation empty() {
        return EMPTY;
    }

    Header[] transformHeaders(Header[] headers);
}

