/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.format;

import java.io.IOException;
import java.io.OutputStream;

import io.kroxylicious.filter.transformation.api.Type;

public interface Serializer<T> {

    void accepts(Type<?, ?, ?> type);

    void serialize(T value, OutputStream out) throws IOException;
}
