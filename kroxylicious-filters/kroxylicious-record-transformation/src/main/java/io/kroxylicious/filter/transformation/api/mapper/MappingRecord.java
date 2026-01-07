/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public class MappingRecord<K, KS, KI extends WireSchemaId, V, VS, VI extends WireSchemaId> {
    private List<Header> headers;
    private K key;
    private KS keySchema;
    private KI keySchemaId;
    private V value;
    private VS valueSchema;
    private VI valueSchemaId;

    public MappingRecord(List<Header> headers,
                         K key, KS keySchema, KI keySchemaId,
                         V value, VS valueSchema, VI valueSchemaId) {
        this.headers = headers;
        this.key = key;
        this.keySchema = keySchema;
        this.keySchemaId = keySchemaId;
        this.value = value;
        this.valueSchema = valueSchema;
        this.valueSchemaId = valueSchemaId;
    }

    public K key() {
        return key;
    }

    public KI keySchemaId() {
        return keySchemaId;
    }

    public KS keySchema() {
        return keySchema;
    }

    public void withKey(K key, KI keySchemaId, KS keySchema) {
        this.key = key;
        this.keySchema = keySchema;
        this.keySchemaId = keySchemaId;
    }

    public V value() {
        return value;
    }

    public VS valueSchema() {
        return valueSchema;
    }

    public VI valueSchemaId() {
        return valueSchemaId;
    }

    public void withValue(V value, VI valueSchemaId, VS valueSchema) {
        this.value = value;
        this.valueSchema = valueSchema;
        this.valueSchemaId = valueSchemaId;
    }

    public List<Header> headers() {
        return headers;
    }
}
