/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

import org.apache.kafka.common.record.Record;

/**
 * Something that receives the result of an encryption or decryption operation
 */
public interface Receiver extends BiConsumer<ByteBuffer, Record> {
    /**
     * Receive the ciphertext (encryption) or the plaintext (decryption) associated with the given record..
     * @param buffer The ciphertext or plaintext buffer.
     * @param kafkaRecord The record on which to base the revised record
     */
    // TODO this doesn't compose: We can't use this interface to encrypt both the value and the key, for example
    @Override
    void accept(ByteBuffer buffer, Record kafkaRecord);
}
