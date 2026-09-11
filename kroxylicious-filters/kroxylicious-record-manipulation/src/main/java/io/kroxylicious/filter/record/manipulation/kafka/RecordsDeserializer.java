/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import java.util.function.Function;

import io.kroxylicious.kafka.common.record.internal.BaseRecords;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.transform.RecordStream;

public class RecordsDeserializer implements Function<BaseRecords, RecordStream<Void>> {

    @Override
    public RecordStream<Void> apply(BaseRecords baseRecords) {
        return RecordStream.ofRecords((MemoryRecords) baseRecords);
    }

}
