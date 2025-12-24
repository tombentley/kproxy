/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.format;

import java.io.IOException;
import java.io.InputStream;

import org.apache.kafka.common.header.Header;

public interface Deserializer<T> {

    Class<T> returnedType();

    T deserialize(Header[] headers, InputStream in) throws IOException;
}
