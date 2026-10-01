/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;

public record TopicName(@Nullable String is,
                        @Nullable Set<String> isIn,
                        @Nullable String matches) {
    public TopicName {
        if (is != null) {
            if (isIn != null || matches != null) {
                throw new IllegalArgumentException("Exactly one of `is`, `isIn` and `matches` is required");
            }
        }
        else if (isIn != null) {
            if (matches != null) {
                throw new IllegalArgumentException("Exactly one of `is`, `isIn` and `matches` is required");
            }
        }
        else if (matches == null) {
            throw new IllegalArgumentException("Exactly one of `is`, `isIn` and `matches` is required");
        }
    }
}
