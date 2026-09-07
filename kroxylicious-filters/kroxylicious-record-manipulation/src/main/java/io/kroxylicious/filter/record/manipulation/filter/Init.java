/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.nio.ByteBuffer;

import org.apache.kafka.common.record.Record;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;

public record Init(String topic,
                   Direction direction,
                   BaseTypedOp<Record, Long> recordTimestampPipeline,
                   BaseTypedOp<Record, ByteBuffer> recordKeyPipeline,
                   BaseTypedOp<Record, ByteBuffer> recordValuePipeline) {
}
