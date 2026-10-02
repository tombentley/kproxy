/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import java.nio.ByteBuffer;

import io.kroxylicious.transform.service.BufferTransform;
import io.kroxylicious.transform.service.KafkaRecord;

public class RecordKeyTransform implements BufferTransform {
    @Override
    public ByteBuffer apply(KafkaRecord record) {
        return record.key();
    }
}
