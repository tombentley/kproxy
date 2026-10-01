/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

/**
 * A schema-registry {@code globalId}: the id embedded in the magic-byte framing of a
 * record's key or value, identifying a specific schema artifact + version.
 */
public record GlobalId(long value) {}
