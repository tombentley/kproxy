/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import java.nio.ByteBuffer;

import io.kroxylicious.filter.record.manipulation.filter.transform.BufferTransform;
import io.kroxylicious.kafka.common.record.internal.Record;

public class NullBufferTransform implements BufferTransform {
    @Override
    public ByteBuffer apply(Record record) {
        return null;
    }
}
