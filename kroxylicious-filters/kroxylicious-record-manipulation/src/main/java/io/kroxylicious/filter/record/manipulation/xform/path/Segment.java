/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.List;

public sealed interface Segment {
    record Child(List<Selector> selectors) implements Segment {
        Child(Selector selector) {
            this(List.of(selector));
        }

        @Override
        public String toString() {
            return selectors.toString();
        }
    }

    record Descendant(List<Selector> selectors) implements Segment {
        Descendant(Selector selector) {
            this(List.of(selector));
        }

        @Override
        public String toString() {
            return ".." +
                    selectors;
        }
    }

}
