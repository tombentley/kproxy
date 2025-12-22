/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;

public interface DatumMapper<T, U> {

    Class<T> acceptedType();

    Class<U> returnedType();

    Datum<U> transform(Datum<T> datum) throws IOException;
}
