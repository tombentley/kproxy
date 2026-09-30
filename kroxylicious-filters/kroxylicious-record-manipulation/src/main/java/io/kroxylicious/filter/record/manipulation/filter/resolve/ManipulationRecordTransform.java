/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.nio.ByteBuffer;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.kafka.common.record.internal.RecordBatch;
import io.kroxylicious.kafka.transform.RecordTransform;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The state-aware serializer body: a {@link RecordTransform} whose per-record state is the
 * {@link CompiledManipulation} that matched that record (or {@code null} for no match). Matched records
 * have their {@code then} transforms applied, with the request's {@link ResolvedEntities} in scope;
 * unmatched records pass through unchanged.
 */
final class ManipulationRecordTransform implements RecordTransform<CompiledManipulation> {

    private final ResolvedEntities resolved;
    @Nullable
    private CompiledManipulation current;

    ManipulationRecordTransform(ResolvedEntities resolved) {
        this.resolved = resolved;
    }

    @Override
    public void initBatch(RecordBatch batch) {
    }

    @Override
    public void init(@Nullable CompiledManipulation state, Record record) {
        this.current = state;
    }

    @Override
    public void resetAfterTransform(@Nullable CompiledManipulation state, Record record) {
        this.current = null;
    }

    @Override
    public long transformOffset(Record record) {
        return record.offset();
    }

    @Override
    public long transformTimestamp(Record record) {
        if (current != null && current.newTimestamp() != null) {
            return current.newTimestamp().apply(record);
        }
        return record.timestamp();
    }

    @Nullable
    @Override
    public ByteBuffer transformKey(Record record) {
        if (current != null && current.newKey() != null) {
            return current.newKey().apply(record, resolved);
        }
        return record.key();
    }

    @Nullable
    @Override
    public ByteBuffer transformValue(Record record) {
        if (current != null && current.newValue() != null) {
            return current.newValue().apply(record, resolved);
        }
        return record.value();
    }

    @Nullable
    @Override
    public Header[] transformHeaders(Record record) {
        return record.headers();
    }
}
