/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.format;

import java.io.IOException;
import java.io.InputStream;

public interface Deserializer<T> {

    Class<T> returnedType();

    T deserialize(InputStream in) throws IOException;
}
