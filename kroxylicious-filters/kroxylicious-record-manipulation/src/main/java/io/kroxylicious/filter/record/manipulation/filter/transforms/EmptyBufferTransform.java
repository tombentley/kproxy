/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import java.nio.ByteBuffer;

import io.kroxylicious.transform.records.RecordView;
import io.kroxylicious.transform.service.BufferTransform;

public class EmptyBufferTransform implements BufferTransform {
    @Override
    public ByteBuffer apply(RecordView record) {
        // TODO is an empty buffer threadsafe? I'd _think_ so.
        ByteBuffer allocate = ByteBuffer.allocate(0);
        return allocate;
    }
}
