/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.List;

public sealed interface Segment<N> {
    record Child<N>(List<Selector<N>> selectors) implements Segment<N> {
        Child(Selector<N> selector) {
            this(List.of(selector));
        }

        @Override
        public String toString() {
            return selectors.toString();
        }
    }

    record Descendant<N>(List<Selector<N>> selectors) implements Segment<N> {
        Descendant(Selector<N> selector) {
            this(List.of(selector));
        }

        @Override
        public String toString() {
            return ".." +
                    selectors;
        }
    }

}
