/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * Resolves a set of distinct lookup ids to their resolved content, asynchronously.
 *
 * <p>This is the second of the three collaborators that make up a {@link Facet}. Implementations are
 * expected to deduplicate (the caller passes a {@link Set} of distinct ids) and to cache resolved
 * values across invocations so that repeated requests for the same id do not hit the backing system.
 * See {@link CachingEntityResolver}.</p>
 *
 * <p>Decoupling resolution behind this interface is what keeps the pipeline independent of any
 * particular backing system: the same machinery serves a schema registry or a KMS by supplying a
 * different resolver.</p>
 *
 * @param <ID> the type of the lookup id.
 * @param <CONTENT> the type of the resolved content.
 */
public interface EntityResolver<ID, CONTENT> {

    /**
     * Resolve the given distinct ids.
     * @param ids the distinct ids to resolve (may be empty).
     * @return a stage completing with a map from each resolvable id to its content.
     */
    CompletionStage<Map<ID, CONTENT>> resolveAll(Set<ID> ids);
}
