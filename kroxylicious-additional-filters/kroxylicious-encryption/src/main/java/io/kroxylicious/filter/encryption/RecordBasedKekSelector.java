/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import org.apache.kafka.common.record.Record;

/**
 * KEK selection based on the record.
 * For example, this could be used to implement an "encryption key per record key" strategy.
 * @param <K> the type of key.
 */
public abstract non-sealed class RecordBasedKekSelector<K> implements KekSelector {
    /**
     * Returns the key to use for the given {@code kafkaRecord} in the topic with the given name.
     * @param topicName The name of the topic that the given {@code kafkaRecord} is destined for.
     * @param kafkaRecord The record.
     * @return
     */
    public abstract K keyRef(String topicName, Record kafkaRecord);
}
