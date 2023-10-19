/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.coordinator;

import org.apache.kafka.common.serialization.Deserializer;

public class DekRecordSerializer<K, E> implements Deserializer<DekRecord<K, E>> {

    private final Deserializer<E> edekDeserializer;

    public DekRecordSerializer(Deserializer<E> edekDeserializer) {
        this.edekDeserializer = edekDeserializer;
    }

    @Override
    public DekRecord<K, E> deserialize(String topic, byte[] data) {
        // TODO
        return null;
    }
}