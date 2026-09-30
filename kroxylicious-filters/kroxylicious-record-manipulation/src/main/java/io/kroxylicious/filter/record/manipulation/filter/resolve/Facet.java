/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

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
public record Facet<ID, CONTENT>(String discriminator,
                                 LookupKeyExtractor<ID> extractor,
                                 EntityResolver<ID, CONTENT> resolver) {}
