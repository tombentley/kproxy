/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import org.apache.kafka.common.record.MemoryRecordsBuilder;
import org.apache.kafka.common.record.Record;

import java.nio.ByteBuffer;

public interface Receiver {
    /**
     * Append a revised record, based on the given {@code kafkaRecord}, but with the relevant part
     * replaced with the given encrypted {@code buffer}, to the given {@code builder}.
     * @param kafkaRecord The record on which to base the revised record
     * @param buffer The encrypted buffer.
     */
    // TODO this doesn't compose: We can't use this interface to encrypt both the value and the key, for example
    void receive(Record kafkaRecord, ByteBuffer buffer);
}
