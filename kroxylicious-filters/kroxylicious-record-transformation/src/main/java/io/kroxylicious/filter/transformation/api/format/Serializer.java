/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.format;

import java.io.IOException;
import java.io.OutputStream;

import io.kroxylicious.filter.transformation.api.Type;

import edu.umd.cs.findbugs.annotations.Nullable;

public interface Serializer<T> {

    void accepts(Type<?, ?, ?> type);

    void serialize(@Nullable T value, OutputStream out) throws IOException;
}
