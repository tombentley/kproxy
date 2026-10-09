/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.kroxylicious.transform.records.RecordView;
import io.kroxylicious.transform.service.BufferTransform;

public class RecordValueTransform implements BufferTransform {
    @Override
    public CompletionStage<ByteBuffer> apply(RecordView record) {
        return CompletableFuture.completedStage(record.value());
    }
}
