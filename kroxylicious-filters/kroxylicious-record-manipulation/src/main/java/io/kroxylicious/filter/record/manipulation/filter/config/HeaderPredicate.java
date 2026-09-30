/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import java.util.Set;

public record HeaderPredicate(
                              String withKey,
                              String withKeyMatching,
                              Set<String> withKeyIn) {}
