/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;

public record TopicName(@Nullable String withName,
                        @Nullable Set<String> withNameIn,
                        @Nullable String withNameMatching) {

    public static final String MSG = "Exactly one of `withName`, `withNameIn` and `withNameMatching` is required";

    public TopicName {
        if (withName != null) {
            if (withNameIn != null || withNameMatching != null) {
                throw new IllegalArgumentException(MSG);
            }
        }
        else if (withNameIn != null) {
            if (withNameMatching != null) {
                throw new IllegalArgumentException(MSG);
            }
        }
        else if (withNameMatching == null) {
            throw new IllegalArgumentException(MSG);
        }
    }

    public static TopicName is(String name) {
        return new TopicName(name, null, null);
    }

    public static TopicName isIn(Set<String> names) {
        return new TopicName(null, names, null);
    }

    public static TopicName matches(String pattern) {
        return new TopicName(null, null, pattern);
    }

}
