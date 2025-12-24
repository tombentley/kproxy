/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.format;

import java.io.IOException;
import java.io.OutputStream;

public interface Serializer<T> {

    Class<T> acceptedType();

    void serialize(T value, OutputStream out) throws IOException;
}
