/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import io.kroxylicious.filter.transformation.api.Type;

/**
 * <p>A typed, unary function for transformating values.</p>
 */
public interface TypeCheckable {

    Type<?, ?, ?> typeCheck(Type<?, ?, ?> type);

}
