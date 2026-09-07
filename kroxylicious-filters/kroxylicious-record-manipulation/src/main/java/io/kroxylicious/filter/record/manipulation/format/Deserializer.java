/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format;

import java.nio.ByteBuffer;

public interface Deserializer<T> {
    T deserialize(ByteBuffer byteBuffer);
}
