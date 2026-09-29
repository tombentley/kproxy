/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transform;

import java.nio.ByteBuffer;

import io.kroxylicious.kafka.common.record.internal.Record;

import edu.umd.cs.findbugs.annotations.Nullable;

public interface BufferTransform {

    @Nullable
    ByteBuffer apply(Record record);
}
