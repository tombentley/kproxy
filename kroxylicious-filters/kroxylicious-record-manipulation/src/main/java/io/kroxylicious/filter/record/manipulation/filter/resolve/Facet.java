/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.nio.ByteBuffer;
import java.util.function.Function;

import io.kroxylicious.filter.record.manipulation.filter.apicurio.ContentId;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.ContentResult;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.GlobalId;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.GlobalIdExtractor;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.SchemaFacets;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.SchemaRegistryClient;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.VersionsResult;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.transform.service.DependencyKey;

/**
 * A "related entity" that records may reference and which must be resolved from an external system
 * before a manipulation can be selected or applied.
 *
 * <p>A facet bundles the three collaborators the user identified:</p>
 * <ul>
 *   <li>a {@link LookupKeyExtractor} — record &rarr; id;</li>
 *   <li>an {@link EntityResolver} — id &rarr; content (deduplicated and cached);</li>
 *   <li>a {@code discriminator} — a stable name used to key the resolved values in
 *       {@link ResolvedEntities} so that a transform can retrieve what it needs.</li>
 * </ul>
 *
 * <p>A two-stage resolution (e.g. schema {@code globalId} &rarr; version metadata &rarr; schema
 * content, keyed by {@code contentId}) is modelled as two facets; the chaining of one stage's
 * resolved content into the next stage's id is performed by the orchestrator, which knows the
 * relationship. Single-stage resolutions (e.g. EDEK &rarr; DEK) use a single facet.</p>
 *
 * @param <ID> the type of the lookup id.
 * @param <CONTENT> the type of the resolved content.
 * @param discriminator stable name used to key resolved values in {@link ResolvedEntities}.
 * @param extractor extracts the id from a record.
 * @param resolver resolves ids to content.
 */
public record Facet<T, ID, CONTENT>(DependencyKey<ID, CONTENT> discriminator,
                                    LookupKeyExtractor<T, ID> extractor,
                                    EntityResolver<ID, CONTENT> resolver) {

    public static Facet<Record, GlobalId, VersionsResult> schemaVersions(SchemaRegistryClient client, Function<Record, ByteBuffer> fn) {
        return new Facet<Record, GlobalId, VersionsResult>(
                SchemaFacets.VERSIONS_DEPENDENCY,
                new GlobalIdExtractor(fn),
                new CachingEntityResolver<>(
                        globalId -> client.versionsForGlobalId(globalId).toCompletableFuture(),
                        100));
    }

    public static Facet<VersionsResult, ContentId, ContentResult> schema(SchemaRegistryClient client) {
        return new Facet<VersionsResult, ContentId, ContentResult>(
                new DependencyKey<>(ContentId.class, ContentResult.class),
                (VersionsResult x) -> x.contentId(),
                new CachingEntityResolver<>(
                        contentId -> client.contentById(contentId).toCompletableFuture(),
                        100));
    }

}
