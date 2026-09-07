/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.utils.ByteBufferOutputStream;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.common.Functional;
import io.kroxylicious.kafka.transform.RecordStream;
import io.kroxylicious.kafka.transform.RecordTransform;

import edu.umd.cs.findbugs.annotations.Nullable;

public class RecordsSerializer {

    private final Random prng = new Random();
    private final BaseTypedOp<Record, Long> recordTimestampPipeline;
    private final BaseTypedOp<Record, ByteBuffer> recordKeyPipeline;
    private final BaseTypedOp<Record, ByteBuffer> recordValuePipeline;

    public RecordsSerializer(
            BaseTypedOp<Record, Long> recordTimestampPipeline,
            BaseTypedOp<Record, ByteBuffer> recordKeyPipeline,
            BaseTypedOp<Record, ByteBuffer> recordValuePipeline) {
        if (recordKeyPipeline.inputType() != Record.class) { // TODO actually an assignability check
            throw new RuntimeException("recordKeyPipeline must accept " + Record.class.getName() + " but actually accepts " + recordKeyPipeline.inputType());
        }
        if (recordValuePipeline.inputType() != Record.class) { // TODO actually an assignability check
            throw new RuntimeException("recordValuePipeline must accept " + Record.class.getName() + " but actually accepts " + recordKeyPipeline.inputType());
        }
        if (recordKeyPipeline.outputType() != ByteBuffer.class) { // TODO actually an assignability check
            throw new RuntimeException("recordKeyPipeline must return " + ByteBuffer.class.getName() + " but actually returns " + recordKeyPipeline.outputType());
        }
        if (recordValuePipeline.outputType() != ByteBuffer.class) { // TODO actually an assignability check
            throw new RuntimeException("recordValuePipeline must return " + ByteBuffer.class.getName() + " but actually returns " + recordKeyPipeline.outputType());
        }
        // TODO should check the 2nd type argument is actually Context
        this.recordTimestampPipeline = recordTimestampPipeline;
        this.recordKeyPipeline = recordKeyPipeline;
        this.recordValuePipeline = recordValuePipeline;
    }

    public MemoryRecords apply(RecordStream<TopicPartition> stream) {
        return stream.toMemoryRecords(new ByteBufferOutputStream(ByteBuffer.allocate(1000)),
                new RecordTransform<TopicPartition>() {
                    private TopicPartition topicPartition;
                    private RecordBatch batch;

                    @Override
                    public void initBatch(RecordBatch batch) {
                        this.batch = batch;
                    }

                    @Override
                    public void init(TopicPartition topicPartition, Record record) {
                        this.topicPartition = topicPartition;
                    }

                    @Override
                    public void resetAfterTransform(TopicPartition topicPartition, Record record) {
                        this.topicPartition = null;
                    }

                    private OpContext context(Record record,
                                              Integer index) {
                        long seed = ((long) topicPartition.hashCode()) << 32 | (topicPartition.partition() ^ (batch.baseSequence() + index));
                        prng.setSeed(seed);

                        return new OpContext(prng, null);
                    }

                    @Override
                    public long transformOffset(Record record) {
                        return record.offset();
                    }

                    @Override
                    public long transformTimestamp(Record record) {
                        return Functional.bind(recordTimestampPipeline, context(record, 0)).apply(record);
                    }

                    @Nullable
                    @Override
                    public ByteBuffer transformKey(Record record) {
                        return Functional.bind(recordKeyPipeline, context(record, 0)).apply(record);
                    }

                    @Nullable
                    @Override
                    public ByteBuffer transformValue(Record record) {
                        return Functional.bind(recordValuePipeline, context(record, 0)).apply(record);
                    }

                    @Nullable
                    @Override
                    public Header[] transformHeaders(Record record) {
                        return record.headers();
                    }
                });
    }
}
