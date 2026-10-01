/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.nio.ByteBuffer;
import java.util.function.Function;

import io.kroxylicious.filter.record.manipulation.filter.GlobalId;
import io.kroxylicious.kafka.common.record.internal.Record;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Extracts an Apicurio-style schema {@code globalId} from the magic-byte framing at the start of a
 * record key or value.
 *
 * <p>The framing is a single {@code 0x00} magic byte followed by an 8-byte big-endian
 * {@code globalId}. Buffers that are too short or do not begin with the magic byte yield
 * {@code null} (the record is not schema-framed).</p>
 *
 * <p>The {@code accessor} selects which part of the record to read, i.e. {@code Record::key} or
 * {@code Record::value}.</p>
 */
public final class SchemaIdExtractor implements LookupKeyExtractor<GlobalId> {

    private static final byte MAGIC_BYTE = 0x00;
    /** magic byte (1) + globalId (8). */
    private static final int FRAMING_LENGTH = 1 + Long.BYTES;

    private final Function<Record, ByteBuffer> accessor;

    /**
     * Creates an extractor that reads the framing from the part of the record chosen by {@code accessor}.
     * @param accessor selects the framed buffer from a record (e.g. {@code Record::key}).
     */
    public SchemaIdExtractor(Function<Record, ByteBuffer> accessor) {
        this.accessor = accessor;
    }

    @Override
    @Nullable
    public GlobalId extract(Record record) {
        ByteBuffer buffer = accessor.apply(record);
        if (buffer == null) {
            return null;
        }
        int position = buffer.position();
        if (buffer.remaining() >= FRAMING_LENGTH && buffer.get(position) == MAGIC_BYTE) {
            return new GlobalId(buffer.getLong(position + 1));
        }
        return null;
    }
}
