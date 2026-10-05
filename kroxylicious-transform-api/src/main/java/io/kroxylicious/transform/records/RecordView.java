/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.nio.ByteBuffer;

import io.kroxylicious.kafka.common.header.Header;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * An immutable, read-only view of a single record. Key and value are returned as (read-only) {@link ByteBuffer}s
 * that, where possible, slice directly into the source buffer without copying.
 */
public interface RecordView {

    /**
     * {@return the record's absolute offset in the log}
     */
    long offset();

    /**
     * {@return the record's timestamp}
     */
    long timestamp();

    /**
     * {@return the record's producer-assigned sequence number, or -1 if there is none}
     */
    int sequence();

    /**
     * {@return true if the record has a (non-null) key}
     */
    boolean hasKey();

    /**
     * {@return the record's key, or null if there is none}
     */
    @Nullable
    ByteBuffer key();

    /**
     * {@return true if the record has a (non-null) value}
     */
    boolean hasValue();

    /**
     * {@return the record's value, or null if there is none (a tombstone)}
     */
    @Nullable
    ByteBuffer value();

    /**
     * {@return the record's headers (never null; may be empty)}
     */
    Header[] headers();
}
