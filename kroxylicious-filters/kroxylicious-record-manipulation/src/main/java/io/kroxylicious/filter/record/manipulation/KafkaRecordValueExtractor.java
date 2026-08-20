/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation;

import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.kafka.common.record.Record;

class KafkaRecordValueExtractor implements Function<org.apache.kafka.common.record.Record, ByteBuffer> {
    @Override
    public ByteBuffer apply(Record r) {
        return r.value();
    }
}
