/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CachingEntityResolverTest {

    @Test
    void deduplicatesAndCachesLoadsAcrossCalls() {
        // Given
        AtomicInteger loads = new AtomicInteger();
        Function<Long, CompletableFuture<String>> loader = id -> {
            loads.incrementAndGet();
            return CompletableFuture.completedFuture("v" + id);
        };
        CachingEntityResolver<Long, String> resolver = new CachingEntityResolver<>(loader, CachingEntityResolver.UNBOUNDED);

        // When
        Map<Long, String> first = resolver.resolveAll(Set.of(1L, 2L)).toCompletableFuture().join();
        Map<Long, String> second = resolver.resolveAll(Set.of(2L, 3L)).toCompletableFuture().join();

        // Then
        assertThat(first).containsExactlyInAnyOrderEntriesOf(Map.of(1L, "v1", 2L, "v2"));
        assertThat(second).containsExactlyInAnyOrderEntriesOf(Map.of(2L, "v2", 3L, "v3"));
        assertThat(loads).hasValue(3);
    }

    @Test
    void emptyIdSetResolvesWithoutLoading() {
        // Given
        AtomicInteger loads = new AtomicInteger();
        CachingEntityResolver<Long, String> resolver = new CachingEntityResolver<>(id -> {
            loads.incrementAndGet();
            return CompletableFuture.completedFuture("v" + id);
        }, CachingEntityResolver.UNBOUNDED);

        // When
        var result = resolver.resolveAll(Set.of());

        // Then
        assertThat(result).succeedsWithin(Duration.ofSeconds(5))
                .satisfies(map -> assertThat(map).isEmpty());
        assertThat(loads).hasValue(0);
    }
}
