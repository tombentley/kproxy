/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.util.Objects;

import io.kroxylicious.kafka.common.record.internal.CompressionType;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * <p>An immutable, composable description of the <em>batch-level</em> changes that are safe for a proxy to make:
 * the batch compression, and the small band of batch-wide timestamp transforms that preserve the record
 * format's delta encoding.</p>
 *
 * <p>Everything else about a batch's framing (base offset, producer id/epoch, base sequence, last offset delta,
 * …) is immutable: those fields are coupled to broker/client exactly-once semantics and are always preserved
 * verbatim.</p>
 *
 * <p>The two timestamp transforms are mutually exclusive:</p>
 * <ul>
 *     <li>{@link Builder#shiftTimestamps(long)} — add a constant delta to every record's timestamp. Because the
 *         per-record {@code timestampDelta}s are unchanged, this only moves the batch's base/max timestamp;</li>
 *     <li>{@link Builder#setTimestamps(long)} — set every record's timestamp to a single value (all deltas become
 *         zero).</li>
 * </ul>
 */
public final class BatchEdit {

    enum TimestampMode {
        KEEP,
        SHIFT,
        SET
    }

    private static final BatchEdit KEEP = new BatchEdit(null, TimestampMode.KEEP, 0L);

    private final @Nullable CompressionType compression;
    private final TimestampMode timestampMode;
    private final long timestampOperand;

    private BatchEdit(@Nullable CompressionType compression, TimestampMode timestampMode, long timestampOperand) {
        this.compression = compression;
        this.timestampMode = timestampMode;
        this.timestampOperand = timestampOperand;
    }

    /**
     * {@return the identity edit: the batch is kept unchanged}
     */
    public static BatchEdit keep() {
        return KEEP;
    }

    /**
     * Creates a new builder.
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@return true if this edit leaves the batch unchanged}
     */
    public boolean isKeep() {
        return compression == null && timestampMode == TimestampMode.KEEP;
    }

    /**
     * {@return true if this edit changes the batch compression}
     */
    public boolean editsCompression() {
        return compression != null;
    }

    /**
     * {@return true if this edit changes record timestamps}
     */
    public boolean editsTimestamps() {
        return timestampMode != TimestampMode.KEEP;
    }

    /**
     * {@return the target compression, or null to keep the source compression}
     */
    @Nullable
    CompressionType compression() {
        return compression;
    }

    TimestampMode timestampMode() {
        return timestampMode;
    }

    @Override
    public String toString() {
        return "BatchEdit{" +
                "compression=" + compression +
                ", timestampMode=" + timestampMode +
                ", timestampOperand=" + timestampOperand +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BatchEdit batchEdit = (BatchEdit) o;
        return timestampOperand == batchEdit.timestampOperand
                && compression == batchEdit.compression
                && timestampMode == batchEdit.timestampMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(compression, timestampMode, timestampOperand);
    }

    /**
     * Apply the timestamp transform of this edit to a record's original timestamp.
     * @param originalTimestamp the record's original timestamp
     * @return the transformed timestamp
     */
    long transformTimestamp(long originalTimestamp) {
        return switch (timestampMode) {
            case KEEP -> originalTimestamp;
            case SHIFT -> originalTimestamp + timestampOperand;
            case SET -> timestampOperand;
        };
    }

    /**
     * A builder for a {@link BatchEdit}.
     */
    public static final class Builder {

        private @Nullable CompressionType compression;
        private TimestampMode timestampMode = TimestampMode.KEEP;
        private long timestampOperand;

        private Builder() {
        }

        /**
         * Recompress the batch with the given codec on write. Omit to keep the source compression.
         * @param compression the target compression codec
         * @return this builder
         */
        public Builder compression(CompressionType compression) {
            this.compression = Objects.requireNonNull(compression);
            return this;
        }

        /**
         * Add a constant delta to every record's timestamp. Mutually exclusive with {@link #setTimestamps(long)}.
         * @param deltaMillis the delta to add (may be negative)
         * @return this builder
         */
        public Builder shiftTimestamps(long deltaMillis) {
            this.timestampMode = TimestampMode.SHIFT;
            this.timestampOperand = deltaMillis;
            return this;
        }

        /**
         * Set every record's timestamp to the given value. Mutually exclusive with {@link #shiftTimestamps(long)}.
         * @param timestampMillis the timestamp to set
         * @return this builder
         */
        public Builder setTimestamps(long timestampMillis) {
            this.timestampMode = TimestampMode.SET;
            this.timestampOperand = timestampMillis;
            return this;
        }

        /**
         * Builds the composed batch edit.
         * @return the composed {@link BatchEdit}
         */
        public BatchEdit build() {
            return new BatchEdit(compression, timestampMode, timestampOperand);
        }
    }
}
