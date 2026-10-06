/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.kroxylicious.transform.records.AsyncConsumerRecordTransform;
import io.kroxylicious.transform.records.BatchView;
import io.kroxylicious.transform.records.ConsumerRecordEdit;
import io.kroxylicious.transform.records.RecordView;

/**
 * The consume-path ({@code OUT_OF_CLUSTER}, i.e. fetch / share-fetch) record transform. For each record it
 * selects the first applicable manipulation (by header predicate) and applies that manipulation's key/value
 * transforms, leaving the record unchanged when nothing matches so the engine can copy it verbatim.
 *
 * <p>The edits are computed synchronously, so an already-completed stage is always returned; the signature is
 * asynchronous to leave room for per-record dependency resolution (schemas, DEKs) in a later iteration.</p>
 */
public final class ManipulationConsumerTransform extends AbstractManipulationTransform implements AsyncConsumerRecordTransform {

    public ManipulationConsumerTransform(List<CompiledManipulation<?, ?, ?>> manipulations) {
        super(manipulations);
    }

    @Override
    public CompletionStage<ConsumerRecordEdit> transform(BatchView batch, RecordView record) {
        CompiledManipulation<?, ?, ?> manipulation = firstMatch(record.headers());
        if (manipulation == null) {
            return CompletableFuture.completedFuture(ConsumerRecordEdit.keep());
        }
        ConsumerRecordEdit.Builder builder = ConsumerRecordEdit.builder();
        if (manipulation.newKey() != null) {
            ByteBuffer newKey = manipulation.newKey().apply(record);
            if (newKey == null) {
                builder.clearKey();
            }
            else {
                builder.key(newKey);
            }
        }
        if (manipulation.newValue() != null) {
            ByteBuffer newValue = manipulation.newValue().apply(record);
            if (newValue == null) {
                builder.clearValue();
            }
            else {
                builder.value(newValue);
            }
        }
        return CompletableFuture.completedFuture(builder.build());
    }
}
