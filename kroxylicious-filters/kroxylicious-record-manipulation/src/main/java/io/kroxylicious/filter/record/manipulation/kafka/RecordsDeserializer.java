/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import java.util.Random;
import java.util.function.Function;

import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.kafka.common.record.internal.BaseRecords;
import io.kroxylicious.kafka.transform.RecordStream;

public class RecordsDeserializer implements Function<BaseRecords, RecordStream<Context>> {

    private final String topic;
    private final int partition;
    private final Random prng = new Random();

    public RecordsDeserializer(String topic, int partition) {
        this.topic = topic;
        this.partition = partition;
    }

    private Context context(RecordBatch batch, Record record, Integer index) {
        long seed = ((long) topic.hashCode()) << 32 | (partition ^ (batch.baseSequence() + index));
        prng.setSeed(seed);
        return new Context(prng, null);
    }

    @Override
    public RecordStream<Context> apply(BaseRecords baseRecords) {
        return RecordStream.ofRecordsWithIndex((MemoryRecords) baseRecords).mapPerRecord(this::context);
    }

}
