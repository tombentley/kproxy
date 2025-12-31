/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.model;

import io.kroxylicious.filter.transformation.api.mapper.HeaderMapping;

/**
 * A collection of transformations to be applied to a record.
 */
public record RecordTransform(

        HeaderMapping headerTransformation,

        DataTransform keyTransform,

        DataTransform valueTransform
        ) {

}
