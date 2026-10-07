/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import java.util.stream.Stream;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.transform.service.HeadersTransform;

public class RemoveAll implements HeadersTransform {

    private final String key;

    RemoveAll(String key) {
        this.key = key;
    }

    @Override
    public Stream<Header> transform(Stream<Header> headers) {
        return headers.filter(header -> header.key().equals(key));
    }
}
