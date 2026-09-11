/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.nio.ByteBuffer;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.kafka.common.record.internal.Record;

public record Init(String topic,
                   Direction direction,
                   BaseTypedOp<Record, Long> recordTimestampPipeline,
                   BaseTypedOp<Record, ByteBuffer> recordKeyPipeline,
                   BaseTypedOp<Record, ByteBuffer> recordValuePipeline) {}
