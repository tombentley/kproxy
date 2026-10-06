/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.Nullable;

public record PrincipalMatch(@JsonProperty(required = true) String withType,
                             @Nullable String withName,
                             @Nullable Set<String> withNameIn,
                             @Nullable String withNameMatching) {

    public static final String MSG = "Exactly one of `withName`, `withNameIn` and `withNameMatching` is required";

    public PrincipalMatch {
        Objects.requireNonNull(withType, "withType is required");
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

    public static PrincipalMatch is(String type, String name) {
        return new PrincipalMatch(type, name, null, null);
    }

    public static PrincipalMatch isIn(String type, Set<String> names) {
        return new PrincipalMatch(type, null, names, null);
    }

    public static PrincipalMatch matches(String type, String pattern) {
        return new PrincipalMatch(type, null, null, pattern);
    }
}
