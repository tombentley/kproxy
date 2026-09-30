/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.util.Objects;
import java.util.function.Predicate;

import io.kroxylicious.filter.record.manipulation.filter.Direction;
import io.kroxylicious.filter.record.manipulation.filter.SchemaRegistryClient.VersionsResult;
import io.kroxylicious.filter.record.manipulation.filter.config.HeaderPredicate;
import io.kroxylicious.filter.record.manipulation.filter.config.SchemaRef;
import io.kroxylicious.filter.record.manipulation.filter.config.TopicName;
import io.kroxylicious.transform.service.TimestampTransform;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The runtime form of a configured manipulation: a compiled {@code when} matcher (topic predicate,
 * synchronous header predicate, and optional key/value schema criteria) plus a compiled {@code then}
 * (timestamp / key / value transforms).
 *
 * <p>Schema criteria are evaluated against {@link VersionsResult}s that have been resolved from the
 * schema registry; the header predicate is evaluated synchronously against the record while it is
 * being traversed; topic and direction are pre-filtered before a work item reaches the orchestrator.</p>
 *
 * @param direction the direction this manipulation applies to.
 * @param topicMatcher tests whether a topic name is in scope.
 * @param headerPredicate an optional synchronous predicate on record headers.
 * @param keySchema optional criteria on the record key's schema.
 * @param valueSchema optional criteria on the record value's schema.
 * @param newTimestamp optional replacement timestamp transform ({@code null} = keep original).
 * @param newKey optional replacement key transform ({@code null} = keep original).
 * @param newValue optional replacement value transform ({@code null} = keep original).
 */
public record CompiledManipulation(
                                   Direction direction,
                                   Predicate<String> topicMatcher,
                                   @Nullable HeaderPredicate headerPredicate,
                                   @Nullable SchemaRef keySchema,
                                   @Nullable SchemaRef valueSchema,
                                   @Nullable TimestampTransform newTimestamp,
                                   @Nullable ContextualBufferTransform newKey,
                                   @Nullable ContextualBufferTransform newValue) {

    /**
     * Reports whether selection depends on the key schema.
     * @return true if this manipulation selects records by their key schema.
     */
    public boolean needsKeySchema() {
        return keySchema != null;
    }

    /**
     * Reports whether selection depends on the value schema.
     * @return true if this manipulation selects records by their value schema.
     */
    public boolean needsValueSchema() {
        return valueSchema != null;
    }

    /**
     * Reports whether this manipulation is in scope for the given topic.
     * @param topicName the topic name.
     * @return true if this manipulation is in scope for the given topic.
     */
    public boolean appliesToTopic(String topicName) {
        return topicMatcher.test(topicName);
    }

    /**
     * Evaluates the (asynchronously resolved) schema part of this manipulation's {@code when} against a
     * record's resolved key/value schema versions.
     *
     * @param keyVersions the resolved version metadata for the record's key schema id, or {@code null}
     *                    if the record has no key schema id or it did not resolve.
     * @param valueVersions the resolved version metadata for the record's value schema id, or
     *                      {@code null}.
     * @return true if the schema criteria are satisfied (trivially true when there are none).
     */
    public boolean matchesSchema(@Nullable VersionsResult keyVersions, @Nullable VersionsResult valueVersions) {
        if (keySchema != null && !matches(keySchema, keyVersions)) {
            return false;
        }
        return valueSchema == null || matches(valueSchema, valueVersions);
    }

    private static boolean matches(SchemaRef ref, @Nullable VersionsResult versions) {
        if (versions == null) {
            return false;
        }
        return Objects.equals(ref.groupId(), versions.groupId())
                && Objects.equals(ref.artifactId(), versions.artifactId())
                && (ref.version() == null || "*".equals(ref.version()) || ref.version().equals(versions.version()));
    }

    /**
     * Builds a topic-name predicate from a {@link TopicName} selector. A {@code null} selector matches
     * every topic.
     * @param topicName the selector, or {@code null} to match all topics.
     * @return the predicate.
     */
    public static Predicate<String> topicPredicate(@Nullable TopicName topicName) {
        if (topicName == null) {
            return name -> true;
        }
        return name -> (topicName.is() != null && topicName.is().equals(name))
                || (topicName.isIn() != null && topicName.isIn().contains(name))
                || (topicName.matches() != null && name.matches(topicName.matches()));
    }
}
