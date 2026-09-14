/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates several {@link Path} expressions in a single traversal of a Jackson tree.
 * <p>
 * Each {@link Path} is tracked by an {@link IndexedPath} recording how far it has matched.
 * When descending into a child we compute the set of {@code IndexedPath}s that can still
 * match somewhere in that child's subtree; if the set is empty the subtree is pruned.
 * <p>
 * Filter predicates receive the candidate node ({@code @}) and the document root ({@code $}).
 * The traversal holds no mutable state, so a predicate may start a fresh traversal from either
 * node (e.g. to resolve an embedded {@code $}-rooted query) by calling {@link #eval} re-entrantly.
 */
public class PathEvaluator<N> {

    /**
     * A {@link Path} together with the index of the segment it is currently trying to match.
     * Immutable: advancing forks a new instance so sibling branches stay independent.
     */
    record IndexedPath<N>(Path<N> path, int index) {
        IndexedPath(Path<N> path) {
            this(path, 0);
        }

        Segment<N> segment() {
            return path.segments().get(index);
        }

        boolean isFinalSegment() {
            return index == path.segments().size() - 1;
        }

        IndexedPath<N> next() {
            return new IndexedPath<>(path, index + 1);
        }
    }

    private final TreeAdapter<N> adapter;

    public PathEvaluator(TreeAdapter<N> adapter) {
        this.adapter = adapter;
    }

    public void eval(N node, Path<N> path) {
        evalInternal(node, node, List.of(new IndexedPath<>(path)));
    }

    public void eval(N node, List<Path<N>> paths) {
        evalInternal(node, node, paths.stream().map(IndexedPath::new).toList());
    }

    private void evalInternal(N root, N node, List<IndexedPath<N>> active) {
        if (adapter.isObject(node)) {
            for (var entry : adapter.objectProperties(node)) {
                descend(root, entry.propertyValue(), entry.propertyName(), 0, active);
            }
        }
        else if (adapter.isArray(node)) {
            int length = adapter.arrayLength(node);
            for (int i = 0; i < length; i++) {
                descend(root, adapter.arrayItem(node, i), i, length, active);
            }
        }
    }

    private void descend(N root, N child, Object accessor, int length, List<IndexedPath<N>> active) {
        List<IndexedPath<N>> childActive = new ArrayList<>();
        for (var ip : active) {
            switch (ip.segment()) {
                case Segment.Child(var selectors) -> {
                    if (matches(selectors, accessor, child, length, root)) {
                        advance(ip, child, childActive);
                    }
                }
                case Segment.Descendant(var selectors) -> {
                    childActive.add(ip); // '..' stays active for deeper nodes
                    if (matches(selectors, accessor, child, length, root)) {
                        advance(ip, child, childActive); // ...and also completes here
                    }
                }
            }
        }
        if (!childActive.isEmpty()) {
            evalInternal(root, child, childActive);
        }
    }

    private void advance(IndexedPath<N> ip, N child, List<IndexedPath<N>> childActive) {
        if (ip.isFinalSegment()) {
            ip.path().consumer().accept(child); // a match
        }
        else {
            childActive.add(ip.next()); // fork forward one segment
        }
    }

    private static <N> boolean matches(List<Selector<N>> selectors, Object accessor, N child, int length, N root) {
        for (var selector : selectors) {
            boolean matched = switch (selector) {
                case Selector.Name(var name) -> name.equals(accessor); // objects only
                case Selector.Index index -> accessor instanceof Integer i && index.matches(i, length); // arrays only
                case Selector.Children() -> true; // wildcard '*'
                case Selector.Slice slice -> accessor instanceof Integer i && slice.matches(i, length); // arrays only
                case Selector.Filter filter -> filter.matches(child, root); // '?<expr>', evaluated against the candidate node and root
            };
            if (matched) {
                return true;
            }
        }
        return false;
    }

}
