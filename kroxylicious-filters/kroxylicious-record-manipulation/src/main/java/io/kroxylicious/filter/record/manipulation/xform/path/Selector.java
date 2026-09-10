/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;

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
        boolean matches(int i) {
            return index == i;
        }

        @Override
        public String toString() {
            return Integer.toString(index);
        }
    }

    record Slice(int start, int end, int step) implements Selector {
        public Slice(int start, int end) {
            this(start, end, 1);
        }

        boolean matches(int i) {
            return start <= i
                    && i < end
                    && (step == 1 || (i - start % step == 0));
        }

        @Override
        public String toString() {
            return start +
                    ":" + end +
                    ":" + step;
        }
    }

    record Filter(Predicate<JsonNode> predicate) implements Selector {
        @Override
        public String toString() {
            return "?" + predicate;
        }
    }
}
