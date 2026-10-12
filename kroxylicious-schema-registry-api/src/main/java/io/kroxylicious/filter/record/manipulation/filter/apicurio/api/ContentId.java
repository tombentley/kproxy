/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio.api;

/**
 * A schema-registry {@code contentId}: the registry-internal, content-addressable id used
 * to fetch the raw bytes of a schema. Distinct from {@link GlobalId} — the registry may
 * map several {@link GlobalId}s to the same {@code contentId} when their content is
 * identical.
 */
public record ContentId(long value) {}
