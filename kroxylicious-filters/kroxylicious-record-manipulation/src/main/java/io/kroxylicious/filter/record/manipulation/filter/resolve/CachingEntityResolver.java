/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * An {@link EntityResolver} backed by a Caffeine {@link AsyncLoadingCache}.
 *
 * <p>{@link #resolveAll(Set)} delegates to {@link AsyncLoadingCache#getAll(Iterable)}, which
 * deduplicates the requested ids against in-flight and cached entries and issues the supplied
 * {@code loader} only for genuinely missing ids. This is the same batch dedup + cache pattern used by
 * the record-encryption filter's DEK cache.</p>
 *
 * <p>The loader holds the reference to the backing system (e.g. a schema registry client or a KMS),
 * so nothing else in the pipeline is coupled to that system.</p>
 *
 * @param <ID> the type of the lookup id.
 * @param <CONTENT> the type of the resolved content.
 */
public final class CachingEntityResolver<ID, CONTENT> implements EntityResolver<ID, CONTENT> {

    /** Passed as {@code maxItems} to leave the cache unbounded. */
    public static final int UNBOUNDED = -1;

    private final AsyncLoadingCache<ID, CONTENT> cache;

    /**
     * Creates a resolver backed by a Caffeine async loading cache.
     * @param loader loads the content for a single (missing) id, asynchronously.
     * @param maxItems the maximum number of cached entries, or {@link #UNBOUNDED}.
     */
    public CachingEntityResolver(Function<ID, CompletableFuture<CONTENT>> loader, int maxItems) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (maxItems != UNBOUNDED) {
            builder.maximumSize(maxItems);
        }
        this.cache = builder.buildAsync((key, executor) -> loader.apply(key));
    }

    @Override
    public CompletionStage<Map<ID, CONTENT>> resolveAll(Set<ID> ids) {
        if (ids.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }
        return cache.getAll(ids);
    }
}
