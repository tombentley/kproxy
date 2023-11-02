/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import org.apache.kafka.common.record.Record;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A manager of (data) encryption keys supporting encryption and decryption operations,
 * encapsulating access to the data encryption keys.
 * @param <K> The type of KEK id.
 */
public interface KeyManager<K> {

    // TODO define contract for the DekCache attempting to destroy key material when it's no longer being used

    /**
     * Asynchronously encrypt the given {@code recordRequests} using the current DEK for the given KEK, calling the given receiver for each encrypted record
     * @param kekId The KEK id
     * @param receiver The receiver of the encrypted buffers
     * @return A completion stage that completes when all the records in the given {@code recordRequests} have been processed.
     */
    @NonNull
    CompletionStage<Void> encrypt(@NonNull K kekId,
                                  @NonNull Stream<RecordEncryptionRequest> recordRequests,
                                  @NonNull Receiver receiver);

    /**
     * Asynchronously decrypt the given {@code kafkaRecord}, calling the given {@code receiver} with the plaintext
     * @param kafkaRecord The record
     * @param receiver The receiver of the plaintext buffers
     * @return A completion stage that completes when the record has been processed.
     */
    @NonNull
    CompletionStage<Void> decrypt(@NonNull Record kafkaRecord,
                                  @NonNull Receiver receiver);
}
