/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.service;

import java.nio.ByteBuffer;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;

public interface BufferTransform {

    default Set<DependencyKey<?, ?>> dependencies() {
        return Set.of();
    }

    default ByteBuffer apply(KafkaRecord record, Dependencies dependencies) {
        return apply(record);
    }

    @Nullable
    ByteBuffer apply(KafkaRecord record);
}
