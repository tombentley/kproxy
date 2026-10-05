/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.nio.ByteBuffer;
import java.util.Objects;

import io.kroxylicious.kafka.common.header.Header;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The shared field-edit core of {@link ProducerRecordEdit} and {@link ConsumerRecordEdit}: the per-field
 * (key / value / headers) description of what changes, with each field defaulting to "keep the original".
 *
 * <p>This is deliberately package-private: the two public edit types present it as their own symmetric
 * surface, so there is no duplicated logic while the public types remain independent.</p>
 */
final class FieldEdits {

    static final FieldEdits KEEP = new FieldEdits(false, null, false, null, HeaderEdits.keep());

    private final boolean keyEdited;
    private final @Nullable ByteBuffer key;
    private final boolean valueEdited;
    private final @Nullable ByteBuffer value;
    private final HeaderEdits headers;

    private FieldEdits(boolean keyEdited, @Nullable ByteBuffer key,
                       boolean valueEdited, @Nullable ByteBuffer value,
                       HeaderEdits headers) {
        this.keyEdited = keyEdited;
        this.key = key;
        this.valueEdited = valueEdited;
        this.value = value;
        this.headers = headers;
    }

    boolean isKeep() {
        return !keyEdited && !valueEdited && headers.isKeep();
    }

    boolean editsKey() {
        return keyEdited;
    }

    boolean editsValue() {
        return valueEdited;
    }

    boolean editsHeaders() {
        return !headers.isKeep();
    }

    /**
     * Resolves the key to write for the given record.
     * @param record the record being transformed
     * @return the edited key if the key was edited, else the record's original key
     */
    @Nullable
    ByteBuffer resolveKey(RecordView record) {
        return keyEdited ? key : record.key();
    }

    /**
     * Resolves the value to write for the given record.
     * @param record the record being transformed
     * @return the edited value if the value was edited, else the record's original value
     */
    @Nullable
    ByteBuffer resolveValue(RecordView record) {
        return valueEdited ? value : record.value();
    }

    /**
     * Resolves the headers to write for the given record.
     * @param record the record being transformed
     * @return the headers computed by applying the header edits over the record's original headers
     */
    Header[] resolveHeaders(RecordView record) {
        return headers.apply(record.headers());
    }

    static final class Builder {

        private boolean keyEdited;
        private @Nullable ByteBuffer key;
        private boolean valueEdited;
        private @Nullable ByteBuffer value;
        private HeaderEdits headers = HeaderEdits.keep();

        Builder key(ByteBuffer newKey) {
            this.keyEdited = true;
            this.key = Objects.requireNonNull(newKey, "use clearKey() to set a null key");
            return this;
        }

        Builder clearKey() {
            this.keyEdited = true;
            this.key = null;
            return this;
        }

        Builder value(ByteBuffer newValue) {
            this.valueEdited = true;
            this.value = Objects.requireNonNull(newValue, "use clearValue() to set a null value (tombstone)");
            return this;
        }

        Builder clearValue() {
            this.valueEdited = true;
            this.value = null;
            return this;
        }

        Builder headers(HeaderEdits headerEdits) {
            this.headers = Objects.requireNonNull(headerEdits);
            return this;
        }

        FieldEdits build() {
            if (!keyEdited && !valueEdited && headers.isKeep()) {
                return KEEP;
            }
            return new FieldEdits(keyEdited, key, valueEdited, value, headers);
        }
    }
}
