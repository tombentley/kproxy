/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JacksonTree {

    static final class IndexedPath {
        private final Path path;
        private int index;

        IndexedPath(Path path) {
            this.path = path;
            this.index = 0;
        }

        public Path path() {
            return path;
        }

        public int index() {
            return index;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (obj == null || obj.getClass() != this.getClass())
                return false;
            var that = (IndexedPath) obj;
            return Objects.equals(this.path, that.path) &&
                    this.index == that.index;
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, index);
        }

        @Override
        public String toString() {
            return "IndexedPath[" +
                    "path=" + path + ", " +
                    "index=" + index + ']';
        }

        public Segment segment() {
            return path.segments().get(index);
        }

        public boolean isFinalSegment() {
            return index == path.segments().size() - 1;
        }

        public void incrementIndex() {
            index++;
        }
    }

    public void eval(JsonNode node, List<Path> paths) {
        evalInternal(node, paths.stream().map(IndexedPath::new).toList());
    }

    private void evalInternal(JsonNode node, List<IndexedPath> paths) {

        if (node instanceof ObjectNode object) {
            for (var propertyEntry : object.properties()) {
                var property = propertyEntry.getKey();
                final JsonNode child = propertyEntry.getValue();
                extracted(paths, property, child);
            }
        }
        else if (node instanceof ArrayNode array) {
            for (int index = 0; index < array.size(); index++) {
                var child = array.get(index);
                extracted(paths, index, child);
            }
        }
        else {
            // no match
        }
    }

    private void extracted(List<IndexedPath> paths, Object accessor, JsonNode child) {
        for (var path : paths) {

            Segment segment = path.segment();
            boolean isFinalSegment = path.isFinalSegment();
            List<JsonNode> results = new ArrayList<>();

            switch (segment) {
                case Segment.Child(var selectors) -> {
                    for (var selector : selectors) {
                        switch (selector) {
                            case Selector.Name(var name) -> {
                                if (accessor.equals(name)) {
                                    if (isFinalSegment) {
                                        path.path.consumer().accept(child);
                                    }
                                    else {
                                        path.incrementIndex();
                                        results.add(child);
                                    }
                                }
                            }
                            case Selector.Children() -> {
                                if (isFinalSegment) {
                                    path.path.consumer().accept(child);
                                }
                                else {
                                    path.incrementIndex();
                                    results.add(child);
                                }
                            }
                            case Selector.Index(var index) -> {
                                if (accessor.equals(index)) {
                                    if (isFinalSegment) {
                                        path.path.consumer().accept(child);
                                    }
                                    else {
                                        path.incrementIndex();
                                        results.add(child);
                                    }
                                }
                            }
                            case Selector.Slice(var start, var end, var step) -> {
                                if (accessor instanceof Integer i
                                        && start <= i
                                        && i <= end) { // TODO step
                                    if (isFinalSegment) {
                                        path.path.consumer().accept(child);
                                    }
                                    else {
                                        path.incrementIndex();
                                        results.add(child);
                                    }
                                }
                            }
                            case Selector.Filter(var predicate) -> {
                                if (predicate.test(child)) {
                                    if (isFinalSegment) {
                                        path.path.consumer().accept(child);
                                    }
                                    else {
                                        path.incrementIndex();
                                        results.add(child);
                                    }
                                }
                            }
                        }
                    }
                }
                case Segment.Descendant(var selectors) -> {
                    for (var selector : selectors) {
                        switch (selector) {
                            case Selector.Name(var name) -> {
                                if (accessor.equals(name)) {
                                    if (isFinalSegment) {
                                        path.path.consumer().accept(child);
                                    }
                                    else {
                                        path.incrementIndex();
                                    }
                                }
                                results.add(child);
                            }
                            case Selector.Children() -> {
                                if (isFinalSegment) {
                                    path.path.consumer().accept(child);
                                }
                                else {
                                    // path.incrementIndex();
                                }
                                results.add(child);
                            }
                            case Selector.Index(var index) -> {
                                if (accessor.equals(index)) {
                                    if (isFinalSegment) {
                                        path.path.consumer().accept(child);
                                    }
                                    else {
                                        path.incrementIndex();
                                    }
                                }
                                results.add(child);
                            }
                            case Selector.Slice(var start, var end, var step) -> {
                                if (accessor instanceof Integer i
                                        && start <= i
                                        && i <= end) { // TODO step
                                    if (isFinalSegment) {
                                        path.path.consumer().accept(child);
                                    }
                                    else {
                                        path.incrementIndex();
                                    }
                                }
                                results.add(child);
                            }
                            case Selector.Filter(var predicate) -> {
                                if (predicate.test(child)) {
                                    if (isFinalSegment) {
                                        path.path.consumer().accept(child);
                                    }
                                    else {
                                        path.incrementIndex();
                                    }
                                    results.add(child);
                                }
                            }
                        }
                    }
                }
            }
            for (var r : results) {
                evalInternal(r, paths);
            }
        }
    }

}
