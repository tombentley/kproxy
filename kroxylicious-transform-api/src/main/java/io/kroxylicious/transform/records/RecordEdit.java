/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;

import io.kroxylicious.kafka.common.header.Header;

/**
 * <p>An immutable, composable description of the changes to make to a record. The editable fields are the record's key,
 * value and headers; each defaults to "keep the original".</p>
 *
 * <p>A consumer record may also be {@link #drop() dropped}. Dropping is safe on the consume path — it mirrors
 * what the broker's log cleaner does to compacted topics — but hazardous on produce, so it exists only here
 * and not on {@link RecordEdit}.</p>
 *
 * <p>Record timestamps cannot be changed individually , but some bulk transformations are supported, see {@link BatchEdit}.</p>
 * <p>Offset, sequence and producer fields cannot be changed; survivors of a drop keep their original offsets.</p>
 */
public final class RecordEdit {

    private static final RecordEdit KEEP = new RecordEdit(FieldEdits.KEEP, false);
    private static final RecordEdit DROP = new RecordEdit(FieldEdits.KEEP, true);

    private final FieldEdits fields;
    private final boolean drop;

    private RecordEdit(FieldEdits fields, boolean drop) {
        this.fields = fields;
        this.drop = drop;
    }

    /**
     * {@return the identity edit: the record is kept unchanged (enabling a verbatim byte copy on write)}
     */
    public static RecordEdit keep() {
        return KEEP;
    }

    /**
     * Shorthand for {@code builder().value(newValue).build()}.
     * @param newValue the new (non-null) value
     * @return the edit
     */
    public static RecordEdit value(ByteBuffer newValue) {
        return builder().value(newValue).build();
    }

    /**
     * Drop the record from the output. Safe on the consume path (mirrors log compaction); survivors keep
     * their original offsets.
     * @return the edit
     */
    public static RecordEdit drop() {
        return DROP;
    }

    /**
     * Creates a new builder.
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@return true if this edit leaves the record unchanged}
     */
    public boolean isKeep() {
        return !drop && fields.isKeep();
    }

    /**
     * {@return true if this edit drops the record}
     */
    public boolean isDrop() {
        return drop;
    }

    /**
     * {@return true if this edit changes the record's key}
     */
    public boolean editsKey() {
        return fields.editsKey();
    }

    /**
     * {@return true if this edit changes the record's value}
     */
    public boolean editsValue() {
        return fields.editsValue();
    }

    /**
     * {@return true if this edit changes the record's headers}
     */
    public boolean editsHeaders() {
        return fields.editsHeaders();
    }

    FieldEdits fields() {
        return fields;
    }

    /**
     * A builder for a {@link RecordEdit}. Unset fields default to keeping the original.
     */
    public static final class Builder {

        private final FieldEdits.Builder fields = new FieldEdits.Builder();

        private Builder() {
        }

        /**
         * Replace the record's key. Use {@link #clearKey()} to set a null key.
         * @param newKey the new (non-null) key
         * @return this builder
         */
        public Builder key(ByteBuffer newKey) {
            fields.key(newKey);
            return this;
        }

        /**
         * Set the record's key to null.
         * @return this builder
         */
        public Builder clearKey() {
            fields.clearKey();
            return this;
        }

        /**
         * Replace the record's value. Use {@link #clearValue()} to set a null value (tombstone).
         * @param newValue the new (non-null) value
         * @return this builder
         */
        public Builder value(ByteBuffer newValue) {
            fields.value(newValue);
            return this;
        }

        /**
         * Set the record's value to null (a tombstone).
         * @return this builder
         */
        public Builder clearValue() {
            fields.clearValue();
            return this;
        }

        /**
         * Apply the given header edits over the record's original headers.
         * @param headerEdits the header edits
         * @return this builder
         */
        public Builder headers(HeaderEdits headerEdits) {
            fields.headers(headerEdits);
            return this;
        }

        /**
         * Convenience for building {@link HeaderEdits} inline.
         * @param headerEdits a consumer that configures a {@link HeaderEdits.Builder}
         * @return this builder
         */
        public Builder headers(Consumer<HeaderEdits.Builder> headerEdits) {
            HeaderEdits.Builder b = HeaderEdits.builder();
            headerEdits.accept(b);
            fields.headers(b.build());
            return this;
        }

        /**
         * Replace the record's headers wholesale.
         * @param headers the replacement headers
         * @return this builder
         */
        public Builder setHeaders(List<Header> headers) {
            fields.headers(HeaderEdits.replaceWith(headers));
            return this;
        }

        /**
         * Remove all of the record's headers.
         * @return this builder
         */
        public Builder clearHeaders() {
            fields.headers(HeaderEdits.clear());
            return this;
        }

        /**
         * Builds the composed edit.
         * @return the composed {@link RecordEdit}
         */
        public RecordEdit build() {
            FieldEdits built = fields.build();
            return built.isKeep() ? keep() : new RecordEdit(built, false);
        }
    }
}
