/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

/**
 * A collection of transformations to be applied to a record.
 * @param headerTransformation The transformation to apply to each record's headers.
 * @param keyTransformation The transformation to apply to each record's key.
 * @param valueTransformation The transformation to apply to each record's value.
 */
public record RecordTransformation(
        HeadersTransformation headerTransformation,
        DatumTransformation keyTransformation,
        DatumTransformation valueTransformation) {
}
