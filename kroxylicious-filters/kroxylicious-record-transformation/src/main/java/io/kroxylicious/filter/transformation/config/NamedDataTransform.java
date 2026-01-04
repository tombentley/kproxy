/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.config;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record NamedDataTransform(String name,
                                 @JsonUnwrapped
                                 DataTransform transform) {
}
