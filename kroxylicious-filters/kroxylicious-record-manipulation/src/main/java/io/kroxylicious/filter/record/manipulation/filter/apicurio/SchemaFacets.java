/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio;

import io.kroxylicious.filter.record.manipulation.filter.resolve.CachingEntityResolver;
import io.kroxylicious.filter.record.manipulation.filter.resolve.EntityResolver;
import io.kroxylicious.filter.record.manipulation.filter.resolve.LookupKeyExtractor;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.transform.service.DependencyKey;

/**
 * The schema-registry "related things", wiring a {@link SchemaRegistryClient} behind the generic
 * {@link CachingEntityResolver}s so that the orchestrator never depends on the client directly.
 *
 * <p>Two resolution stages share the Apicurio {@code globalId} space (a {@code globalId} is unique
 * regardless of whether it framed a key or a value), so a single {@link #versionResolver()} serves both
 * the key and value extractors:</p>
 * <ol>
 *   <li>{@code globalId -> } {@link VersionsResult} (group/artifact/version + {@code contentId}) — used
 *       to <em>select</em> a manipulation;</li>
 *   <li>{@code contentId -> } schema bytes — used to <em>apply</em> a schema-aware {@code then}.</li>
 * </ol>
 */
public final class SchemaFacets {

    public static final DependencyKey<ContentId, byte[]> SCHEMA_CONTENT_DEPENDENCY = new DependencyKey<>(ContentId.class, byte[].class);
    public static final DependencyKey<GlobalId, VersionsResult> VERSIONS_DEPENDENCY = new DependencyKey<>(GlobalId.class, VersionsResult.class);

    private final GlobalIdExtractor keyExtractor = new GlobalIdExtractor(Record::key);
    private final GlobalIdExtractor valueExtractor = new GlobalIdExtractor(Record::value);
    private final EntityResolver<GlobalId, VersionsResult> versionResolver;
    private final EntityResolver<ContentId, byte[]> contentResolver;

    /**
     * Creates the schema resolvers backing the key/value extractors and content lookup.
     * @param client the backing schema registry client.
     * @param versionCacheMax max cached version-metadata entries, or {@link CachingEntityResolver#UNBOUNDED}.
     * @param contentCacheMax max cached schema-content entries, or {@link CachingEntityResolver#UNBOUNDED}.
     */
    public SchemaFacets(SchemaRegistryClient client, int versionCacheMax, int contentCacheMax) {
        this.versionResolver = new CachingEntityResolver<>(
                globalId -> client.versionsForGlobalId(globalId).toCompletableFuture(),
                versionCacheMax);
        this.contentResolver = new CachingEntityResolver<>(
                contentId -> client.contentById(contentId).thenApply(ContentResult::contentBytes).toCompletableFuture(),
                contentCacheMax);
    }

    /**
     * Returns the extractor for the key schema {@code globalId}.
     * @return extractor for the key schema {@code globalId}.
     */
    public LookupKeyExtractor<Record, GlobalId> keyExtractor() {
        return keyExtractor;
    }

    /**
     * Returns the extractor for the value schema {@code globalId}.
     * @return extractor for the value schema {@code globalId}.
     */
    public LookupKeyExtractor<Record, GlobalId> valueExtractor() {
        return valueExtractor;
    }

    /**
     * Returns the resolver from {@code globalId} to version metadata (shared by key and value).
     * @return resolver from {@code globalId} to version metadata (shared by key and value).
     */
    public EntityResolver<GlobalId, VersionsResult> versionResolver() {
        return versionResolver;
    }

    /**
     * Returns the resolver from {@code contentId} to schema bytes.
     * @return resolver from {@code contentId} to schema bytes.
     */
    public EntityResolver<ContentId, byte[]> contentResolver() {
        return contentResolver;
    }
}
