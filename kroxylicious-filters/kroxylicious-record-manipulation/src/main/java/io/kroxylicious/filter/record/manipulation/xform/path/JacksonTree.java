/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
public class JacksonTree {

    /**
     * A {@link Path} together with the index of the segment it is currently trying to match.
     * Immutable: advancing forks a new instance so sibling branches stay independent.
     */
    record IndexedPath(Path path, int index) {
        IndexedPath(Path path) {
            this(path, 0);
        }

        Segment segment() {
            return path.segments().get(index);
        }

        boolean isFinalSegment() {
            return index == path.segments().size() - 1;
        }

        IndexedPath next() {
            return new IndexedPath(path, index + 1);
        }
    }

    public void eval(JsonNode node, List<Path> paths) {
        evalInternal(node, node, paths.stream().map(IndexedPath::new).toList());
    }

    private void evalInternal(JsonNode root, JsonNode node, List<IndexedPath> active) {
        if (node instanceof ObjectNode object) {
            for (var entry : object.properties()) {
                descend(root, entry.getValue(), entry.getKey(), 0, active);
            }
        }
        else if (node instanceof ArrayNode array) {
            int length = array.size();
            for (int i = 0; i < length; i++) {
                descend(root, array.get(i), i, length, active);
            }
        }
    }

    private void descend(JsonNode root, JsonNode child, Object accessor, int length, List<IndexedPath> active) {
        List<IndexedPath> childActive = new ArrayList<>();
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

    private void advance(IndexedPath ip, JsonNode child, List<IndexedPath> childActive) {
        if (ip.isFinalSegment()) {
            ip.path().consumer().accept(child); // a match
        }
        else {
            childActive.add(ip.next()); // fork forward one segment
        }
    }

    private static boolean matches(List<Selector> selectors, Object accessor, JsonNode child, int length, JsonNode root) {
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
