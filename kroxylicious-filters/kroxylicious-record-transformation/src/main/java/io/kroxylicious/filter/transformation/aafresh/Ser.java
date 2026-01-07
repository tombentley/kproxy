/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import java.io.IOException;
import java.io.OutputStream;

public interface Ser<T> {

    void serialize(T value, OutputStream out) throws IOException;
}
