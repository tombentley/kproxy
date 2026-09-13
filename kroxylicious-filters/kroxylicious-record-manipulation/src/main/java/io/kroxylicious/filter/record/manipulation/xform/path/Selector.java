/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.function.BiPredicate;

import com.fasterxml.jackson.databind.JsonNode;

import edu.umd.cs.findbugs.annotations.Nullable;

sealed interface Selector {
    record Name(String name) implements Selector {
        boolean matches(String n) {
            return name.equals(n);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    record Children() implements Selector {
        @Override
        public String toString() {
            return "*";
        }
    }

    record Index(int index) implements Selector {
        /** Whether the selector picks index {@code i} of an array of length {@code len}; negatives count from the end (RFC 9535 §2.3.1). */
        boolean matches(int i, int len) {
            return (index < 0 ? len + index : index) == i;
        }

        @Override
        public String toString() {
            return Integer.toString(index);
        }
    }

    record Slice(@Nullable Integer start, @Nullable Integer end, int step) implements Selector {
        Slice(@Nullable Integer start, @Nullable Integer end) {
            this(start, end, 1);
        }

        /** Whether index {@code i} of an array of length {@code len} falls in the slice (RFC 9535 §2.3.4). */
        boolean matches(int i, int len) {
            if (step == 0) {
                return false;
            }
            boolean forwards = step > 0;
            int lo = forwards ? 0 : -1;
            int hi = forwards ? len : len - 1;
            int anchor = clamp(resolve(start, forwards ? 0 : len - 1, len), lo, hi);
            int limit = clamp(resolve(end, forwards ? len : -len - 1, len), lo, hi);
            return (i - anchor) % step == 0
                    && (forwards ? anchor <= i && i < limit : limit < i && i <= anchor);
        }

        /** Supplies the default when a bound is omitted, then maps a negative bound to an offset from {@code len}. */
        private static int resolve(@Nullable Integer bound, int dflt, int len) {
            int b = bound == null ? dflt : bound;
            return b < 0 ? len + b : b;
        }

        private static int clamp(int v, int lo, int hi) {
            return Math.min(Math.max(v, lo), hi);
        }

        @Override
        public String toString() {
            return (start == null ? "" : start.toString())
                    + ":" + (end == null ? "" : end.toString())
                    + (step == 1 ? "" : ":" + step);
        }
    }

    /** A filter selector {@code ?<expr>}; the predicate is applied to the candidate node ({@code @}) and the document root ({@code $}). */
    record Filter(BiPredicate<JsonNode, JsonNode> predicate) implements Selector {
        boolean matches(JsonNode node, JsonNode root) {
            return predicate.test(node, root);
        }

        @Override
        public String toString() {
            return "?" + predicate;
        }
    }
}
