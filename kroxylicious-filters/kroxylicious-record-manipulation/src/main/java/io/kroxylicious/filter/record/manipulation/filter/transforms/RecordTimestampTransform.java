/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import io.kroxylicious.kafka.common.record.internal.Record;

public class RecordTimestampTransform implements io.kroxylicious.filter.record.manipulation.filter.transform.TimestampTransform {
    @Override
    public long apply(Record record) {
        return record.timestamp();
    }
}
