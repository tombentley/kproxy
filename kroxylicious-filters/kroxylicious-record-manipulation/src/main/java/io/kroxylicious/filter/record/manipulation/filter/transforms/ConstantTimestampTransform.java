/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.transform.service.TimestampTransform;

public class ConstantTimestampTransform implements TimestampTransform {

    private final long timestamp;

    public ConstantTimestampTransform(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long apply(Record record) {
        return timestamp;
    }
}
