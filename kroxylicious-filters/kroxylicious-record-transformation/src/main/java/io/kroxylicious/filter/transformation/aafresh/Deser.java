/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import java.io.IOException;
import java.io.InputStream;

public interface Deser<T> {
    T deser(InputStream inputStream) throws IOException;
}
