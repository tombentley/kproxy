/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.header.internals.RecordHeader;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * <p>An immutable, composable description of how a record's headers should change.</p>
 *
 * <p>Kafka permits <em>duplicate</em> header keys and headers are ordered, so removal and replacement come
 * in {@code All}/{@code First}/{@code Last} variants and additions can target either end. Operations are
 * applied in call order over the record's original header list (see {@link #apply(Header[])}).</p>
 *
 * <p>A {@code HeaderEdits} is one of:</p>
 * <ul>
 *     <li>{@link #keep()} — no change (the identity);</li>
 *     <li>a {@link #builder() patch} — an ordered sequence of add/remove/replace operations applied over the
 *         original headers;</li>
 *     <li>a {@link #replaceWith(List) wholesale replacement} (or {@link #clear()}).</li>
 * </ul>
 */
public final class HeaderEdits {

    /** Fallback behaviour for {@code replaceFirst}/{@code replaceLast} when no header with the key exists. */
    public enum WhenAbsent {
        /** Leave the headers unchanged. */
        NOOP,
        /** Add the header at the start. */
        ADD_FIRST,
        /** Add the header at the end. */
        ADD_LAST
    }

    private enum Mode {
        PATCH,
        REPLACE_ALL
    }

    private enum OpType {
        REMOVE_ALL,
        REMOVE_FIRST,
        REMOVE_LAST,
        ADD_FIRST,
        ADD_LAST,
        REPLACE_FIRST,
        REPLACE_LAST
    }

    private static final class Op {
        private final OpType type;
        private final String key;
        private final @Nullable byte[] value;
        private final @Nullable WhenAbsent whenAbsent;

        Op(OpType type, String key, @Nullable byte[] value, @Nullable WhenAbsent whenAbsent) {
            this.type = type;
            this.key = key;
            this.value = value;
            this.whenAbsent = whenAbsent;
        }

        OpType type() {
            return type;
        }

        String key() {
            return key;
        }

        @Nullable
        byte[] value() {
            return value;
        }

        @Nullable
        WhenAbsent whenAbsent() {
            return whenAbsent;
        }
    }

    private static final HeaderEdits KEEP = new HeaderEdits(Mode.PATCH, List.of(), List.of());

    private final Mode mode;
    private final List<Op> ops;
    private final List<Header> replacement;

    private HeaderEdits(Mode mode, List<Op> ops, List<Header> replacement) {
        this.mode = mode;
        this.ops = ops;
        this.replacement = replacement;
    }

    /**
     * {@return the identity edit: the record's headers are kept unchanged}
     */
    public static HeaderEdits keep() {
        return KEEP;
    }

    /**
     * Creates a new builder.
     * @return a new {@link Builder} for composing an ordered patch of header operations
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Replace the record's headers wholesale with the given list.
     * @param headers the replacement headers
     * @return the edit
     */
    public static HeaderEdits replaceWith(List<Header> headers) {
        return new HeaderEdits(Mode.REPLACE_ALL, List.of(), List.copyOf(headers));
    }

    /**
     * Remove every header.
     * @return the edit
     */
    public static HeaderEdits clear() {
        return new HeaderEdits(Mode.REPLACE_ALL, List.of(), List.of());
    }

    /**
     * {@return true if this edit leaves the headers unchanged}
     */
    public boolean isKeep() {
        return mode == Mode.PATCH && ops.isEmpty();
    }

    /**
     * Compute the resulting headers of applying this edit to the given original headers.
     * @param original the record's original headers (never null; may be empty)
     * @return the resulting headers
     */
    public Header[] apply(Header[] original) {
        if (isKeep()) {
            return original;
        }
        if (mode == Mode.REPLACE_ALL) {
            return replacement.toArray(new Header[0]);
        }
        List<Header> result = new ArrayList<>(List.of(original));
        for (Op op : ops) {
            applyOp(result, op);
        }
        return result.toArray(new Header[0]);
    }

    private static void applyOp(List<Header> headers, Op op) {
        switch (op.type()) {
            case REMOVE_ALL -> headers.removeIf(h -> h.key().equals(op.key()));
            case REMOVE_FIRST -> removeFirst(headers, op.key());
            case REMOVE_LAST -> removeLast(headers, op.key());
            case ADD_FIRST -> headers.add(0, new RecordHeader(op.key(), op.value()));
            case ADD_LAST -> headers.add(new RecordHeader(op.key(), op.value()));
            case REPLACE_FIRST -> replace(headers, op, indexOfFirst(headers, op.key()));
            case REPLACE_LAST -> replace(headers, op, indexOfLast(headers, op.key()));
        }
    }

    private static void replace(List<Header> headers, Op op, int index) {
        if (index >= 0) {
            headers.set(index, new RecordHeader(op.key(), op.value()));
        }
        else {
            switch (Objects.requireNonNull(op.whenAbsent())) {
                case NOOP -> {
                    // leave unchanged
                }
                case ADD_FIRST -> headers.add(0, new RecordHeader(op.key(), op.value()));
                case ADD_LAST -> headers.add(new RecordHeader(op.key(), op.value()));
            }
        }
    }

    private static void removeFirst(List<Header> headers, String key) {
        int i = indexOfFirst(headers, key);
        if (i >= 0) {
            headers.remove(i);
        }
    }

    private static void removeLast(List<Header> headers, String key) {
        int i = indexOfLast(headers, key);
        if (i >= 0) {
            headers.remove(i);
        }
    }

    private static int indexOfFirst(List<Header> headers, String key) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).key().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfLast(List<Header> headers, String key) {
        for (ListIterator<Header> it = headers.listIterator(headers.size()); it.hasPrevious();) {
            int i = it.previousIndex();
            if (it.previous().key().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * A builder for an ordered patch of header operations. Operations are recorded in call order and later
     * applied in that order over the record's original header list.
     */
    public static final class Builder {

        private final List<Op> ops = new ArrayList<>();

        private Builder() {
        }

        /**
         * Remove every header with the given key.
         * @param key the header key
         * @return this builder
         */
        public Builder removeAll(String key) {
            ops.add(new Op(OpType.REMOVE_ALL, requireKey(key), null, null));
            return this;
        }

        /**
         * Remove the first header with the given key, if any.
         * @param key the header key
         * @return this builder
         */
        public Builder removeFirst(String key) {
            ops.add(new Op(OpType.REMOVE_FIRST, requireKey(key), null, null));
            return this;
        }

        /**
         * Remove the last header with the given key, if any.
         * @param key the header key
         * @return this builder
         */
        public Builder removeLast(String key) {
            ops.add(new Op(OpType.REMOVE_LAST, requireKey(key), null, null));
            return this;
        }

        /**
         * Add a header at the start of the header list.
         * @param key the header key
         * @param value the header value (may be null)
         * @return this builder
         */
        public Builder addFirst(String key, @Nullable byte[] value) {
            ops.add(new Op(OpType.ADD_FIRST, requireKey(key), value, null));
            return this;
        }

        /**
         * Add a header at the end of the header list.
         * @param key the header key
         * @param value the header value (may be null)
         * @return this builder
         */
        public Builder addLast(String key, @Nullable byte[] value) {
            ops.add(new Op(OpType.ADD_LAST, requireKey(key), value, null));
            return this;
        }

        /**
         * Replace the value of the first header with the given key; if none exists, fall back per
         * {@code whenAbsent}.
         * @param key the header key
         * @param value the new header value (may be null)
         * @param whenAbsent what to do when no header with the key exists
         * @return this builder
         */
        public Builder replaceFirst(String key, @Nullable byte[] value, WhenAbsent whenAbsent) {
            ops.add(new Op(OpType.REPLACE_FIRST, requireKey(key), value, Objects.requireNonNull(whenAbsent)));
            return this;
        }

        /**
         * Replace the value of the last header with the given key; if none exists, fall back per
         * {@code whenAbsent}.
         * @param key the header key
         * @param value the new header value (may be null)
         * @param whenAbsent what to do when no header with the key exists
         * @return this builder
         */
        public Builder replaceLast(String key, @Nullable byte[] value, WhenAbsent whenAbsent) {
            ops.add(new Op(OpType.REPLACE_LAST, requireKey(key), value, Objects.requireNonNull(whenAbsent)));
            return this;
        }

        /**
         * Builds the composed header edits.
         * @return the composed {@link HeaderEdits}
         */
        public HeaderEdits build() {
            return ops.isEmpty() ? keep() : new HeaderEdits(Mode.PATCH, List.copyOf(ops), List.of());
        }

        private static String requireKey(String key) {
            return Objects.requireNonNull(key, "header key must not be null");
        }
    }
}
