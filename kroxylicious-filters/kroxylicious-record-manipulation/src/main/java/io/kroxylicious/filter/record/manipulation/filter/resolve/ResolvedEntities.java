/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The related entities resolved for a single request/response, keyed by facet
 * {@link Facet#discriminator() discriminator} and then by id.
 *
 * <p>This is the context handed to a {@link ContextualBufferTransform} so that a transform can look
 * up the resolved content it needs (e.g. the schema version metadata for a record's {@code globalId},
 * or the schema bytes for a {@code contentId}).</p>
 */
public final class ResolvedEntities {

    private final Map<String, Map<Object, Object>> byDiscriminator;

    private ResolvedEntities(Map<String, Map<Object, Object>> byDiscriminator) {
        this.byDiscriminator = byDiscriminator;
    }

    /**
     * Creates an empty set of resolved entities.
     * @return an empty set of resolved entities.
     */
    public static ResolvedEntities empty() {
        return new ResolvedEntities(Map.of());
    }

    /**
     * Creates a builder for accumulating resolved maps by discriminator.
     * @return a builder for accumulating resolved maps by discriminator.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Look up the resolved content for the given id under the given discriminator.
     * @param discriminator the facet discriminator.
     * @param id the id.
     * @param <ID> the id type.
     * @param <CONTENT> the content type.
     * @return the resolved content, or {@code null} if not present.
     */
    // The CONTENT type parameter appears only in the return position by design: callers know the
    // content type associated with a discriminator, so this getter casts on their behalf (like Map.get).
    @Nullable
    @SuppressWarnings({ "unchecked", "TypeParameterUnusedInFormals" })
    public <ID, CONTENT> CONTENT get(String discriminator, ID id) {
        Map<Object, Object> map = byDiscriminator.get(discriminator);
        return map == null ? null : (CONTENT) map.get(id);
    }

    /**
     * Builder for {@link ResolvedEntities}.
     */
    public static final class Builder {
        private final Map<String, Map<Object, Object>> byDiscriminator = new HashMap<>();

        private Builder() {
        }

        /**
         * Add the resolved map for a discriminator. Later additions for the same discriminator are
         * merged into the earlier map.
         * @param discriminator the facet discriminator.
         * @param resolved the resolved id &rarr; content map.
         * @return this builder.
         */
        @SuppressWarnings("unchecked")
        public Builder put(String discriminator, Map<?, ?> resolved) {
            byDiscriminator.computeIfAbsent(discriminator, k -> new HashMap<>())
                    .putAll((Map<Object, Object>) resolved);
            return this;
        }

        /**
         * Builds the accumulated resolved entities.
         * @return the built {@link ResolvedEntities}.
         */
        public ResolvedEntities build() {
            return new ResolvedEntities(byDiscriminator);
        }
    }
}
