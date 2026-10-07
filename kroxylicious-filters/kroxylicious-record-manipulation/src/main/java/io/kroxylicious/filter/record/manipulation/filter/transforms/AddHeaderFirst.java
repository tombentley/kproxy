/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import java.util.stream.Stream;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.header.internals.RecordHeader;
import io.kroxylicious.transform.service.HeadersTransform;

public class AddHeaderFirst implements HeadersTransform {

    private final String key;
    private final byte[] value;

    AddHeaderFirst(String key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public Stream<Header> transform(Stream<Header> headers) {
        return Stream.concat(
                Stream.of(new RecordHeader(key, value)),
                headers);
    }
}
