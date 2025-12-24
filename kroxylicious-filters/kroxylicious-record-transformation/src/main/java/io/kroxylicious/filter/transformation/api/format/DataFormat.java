/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface DataFormat<T> {

    Class<T> type();
    void serialize(T value, OutputStream out) throws IOException;
    T deserialize(InputStream in) throws IOException;
    // validator validate(TransformationInputStream in)
}
