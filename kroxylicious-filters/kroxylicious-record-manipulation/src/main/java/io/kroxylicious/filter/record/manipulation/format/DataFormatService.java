/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format;

import io.kroxylicious.filter.record.manipulation.common.BaseTypedOp;

/**
 * A factory for {@link DataFormat}s which do not require a schema, such as JSON or XML.
 * @param <T> The Java type used to represent all data using this format
 * @param <S> The schema configuration type. If the format does not require a schema, this can be {@code Void}.
 * @param <O> The operator configuration type
 */
public interface DataFormatService<T, S, O> {

    DataFormat<T> create(S schemaConfiguration);

    BaseTypedOp<T, T> operator(O operatorConfiguration);
}
