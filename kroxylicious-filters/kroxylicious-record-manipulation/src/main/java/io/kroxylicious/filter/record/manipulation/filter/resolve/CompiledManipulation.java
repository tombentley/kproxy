/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import io.kroxylicious.filter.record.manipulation.filter.apicurio.VersionsResult;
import io.kroxylicious.filter.record.manipulation.filter.config.Direction;
import io.kroxylicious.filter.record.manipulation.filter.config.SchemaRef;
import io.kroxylicious.identity.Identity;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.transform.records.BatchEdit;
import io.kroxylicious.transform.records.RecordEdit;
import io.kroxylicious.transform.records.RecordView;
import io.kroxylicious.transform.service.BufferTransform;
import io.kroxylicious.transform.service.BufferTransformService;

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
 * @param batchEdit optional edits to the batch
 * @param newKey optional replacement key transform ({@code null} = keep original).
 * @param newValue optional replacement value transform ({@code null} = keep original).
 */
@SuppressWarnings("removal")
public record CompiledManipulation<I2, I3>(
                                           Direction direction,
                                           Predicate<Identity> subjectGuard,
                                           Predicate<String> topicMatcher,
                                           @Nullable Predicate<Header[]> headerPredicate,
                                           @Nullable SchemaRef keySchema,
                                           @Nullable SchemaRef valueSchema,
                                           BatchEdit batchEdit,
                                           @Nullable BufferTransformService<?, I2> newKeyService,
                                           @Nullable I2 newKeyInitData,
                                           @Nullable BufferTransform newKey,
                                           @Nullable BufferTransformService<?, I3> newValueService,
                                           @Nullable I3 newValueInitData,
                                           @Nullable BufferTransform newValue) {

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

    public void close() {
        try {
            if (newKeyService != null) {
                newKeyService.close(newKeyInitData);
            }
        }
        finally {
            if (newValueService != null) {
                newValueService.close(newValueInitData);
            }
        }
    }

    public CompletionStage<RecordEdit> asEdit(RecordView record) {

        CompletionStage<ByteBuffer> newKeyStage;
        if (newKey() != null) {
            newKeyStage = Objects.requireNonNull(newKey().apply(record));
        }
        else {
            newKeyStage = null;
        }
        CompletionStage<ByteBuffer> newValueStage;
        if (newValue() != null) {
            newValueStage = Objects.requireNonNull(newValue().apply(record));
        }
        else {
            newValueStage = null;
        }

        if (newKeyStage == null && newValueStage == null) {
            return CompletableFuture.completedStage(RecordEdit.keep());
        }
        else if (newKeyStage == null) {
            return newValueStage.thenApply(newValue -> buildForValue(RecordEdit.builder(), newValue).build());
        }
        else if (newValueStage == null) {
            return newKeyStage.thenApply(newValue -> buildForKey(RecordEdit.builder(), newValue).build());
        }
        else {
            return newKeyStage.thenCombine(newValueStage, (newKey, newValue) -> buildForValue(buildForKey(RecordEdit.builder(), newKey), newValue).build());
        }
    }

    private static RecordEdit.Builder buildForValue(RecordEdit.Builder builder, ByteBuffer newValue) {
        if (newValue == null) {
            builder.clearValue();
        }
        else {
            builder.value(newValue);
        }
        return builder;
    }

    private static RecordEdit.Builder buildForKey(RecordEdit.Builder builder, ByteBuffer newKey) {
        if (newKey == null) {
            builder.clearKey();
        }
        else {
            builder.key(newKey);
        }
        return builder;
    }
}
