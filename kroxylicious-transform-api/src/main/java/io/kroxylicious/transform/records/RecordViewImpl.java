/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.nio.ByteBuffer;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.internal.Record;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A {@link RecordView} backed by an inherited {@link Record}. Key and value are the record's own
 * (zero-copy) buffers.
 */
final class RecordViewImpl implements RecordView {

    private final Record record;

    RecordViewImpl(Record record) {
        this.record = record;
    }

    @Override
    public long offset() {
        return record.offset();
    }

    @Override
    public long timestamp() {
        return record.timestamp();
    }

    @Override
    public int sequence() {
        return record.sequence();
    }

    @Override
    public boolean hasKey() {
        return record.hasKey();
    }

    @Override
    @Nullable
    public ByteBuffer key() {
        return record.key();
    }

    @Override
    public boolean hasValue() {
        return record.hasValue();
    }

    @Override
    @Nullable
    public ByteBuffer value() {
        return record.value();
    }

    @Override
    public Header[] headers() {
        return record.headers();
    }
}
