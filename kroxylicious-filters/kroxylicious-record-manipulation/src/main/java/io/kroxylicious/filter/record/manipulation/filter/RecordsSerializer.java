/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.nio.ByteBuffer;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.kafka.common.record.internal.RecordBatch;
import io.kroxylicious.kafka.common.utils.ByteBufferOutputStream;
import io.kroxylicious.kafka.transform.RecordStream;
import io.kroxylicious.kafka.transform.RecordTransform;
import io.kroxylicious.transform.service.BufferTransform;
import io.kroxylicious.transform.service.TimestampTransform;

import edu.umd.cs.findbugs.annotations.Nullable;

public class RecordsSerializer {

    private final TimestampTransform timestampTransform;
    private final BufferTransform keyTransform;
    private final BufferTransform valueTransform;

    public RecordsSerializer(
                             TimestampTransform timestampTransform,
                             BufferTransform keyTransform,
                             BufferTransform valueTransform) {
        this.timestampTransform = timestampTransform;
        this.keyTransform = keyTransform;
        this.valueTransform = valueTransform;
    }

    public <S> MemoryRecords apply(RecordStream<S> stream) {
        return stream.toMemoryRecords(new ByteBufferOutputStream(ByteBuffer.allocate(1000)),
                new RecordTransform<S>() {
                    @Override
                    public void initBatch(RecordBatch batch) {
                    }

                    @Override
                    public void init(S s, Record record) {
                    }

                    @Override
                    public void resetAfterTransform(S s, Record record) {
                    }

                    @Override
                    public long transformOffset(Record record) {
                        return record.offset();
                    }

                    @Override
                    public long transformTimestamp(Record record) {
                        return timestampTransform.apply(record);
                    }

                    @Nullable
                    @Override
                    public ByteBuffer transformKey(Record record) {
                        return keyTransform.apply(record);
                    }

                    @Nullable
                    @Override
                    public ByteBuffer transformValue(Record record) {
                        return valueTransform.apply(record);
                    }

                    @Nullable
                    @Override
                    public Header[] transformHeaders(Record record) {
                        return record.headers();
                    }
                });
    }
}
