/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.Record;

public record HeaderDataLocation(String key) {
    @Override
    public ByteBuffer buffer(Record record) {
        return Arrays.stream(record.headers())
                .filter(header -> header.key().equals(key))
                .findFirst()
                .map(Header::value)
                .map(ByteBuffer::wrap)
                .orElse(null);
    }
}
