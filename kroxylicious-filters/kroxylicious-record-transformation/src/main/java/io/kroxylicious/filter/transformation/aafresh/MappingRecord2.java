/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import java.util.List;

import org.apache.kafka.common.header.Header;

public class MappingRecord2<K, V> {
    private List<Header> headers;
    private K key;
    private V value;

    public MappingRecord2(List<Header> headers,
                         K key,
                         V value) {
        this.headers = headers;
        this.key = key;
        this.value = value;
    }

    public K key() {
        return key;
    }


    public void withKey(K key) {
        this.key = key;
    }

    public V value() {
        return value;
    }

    public void withValue(V value) {
        this.value = value;
    }

    public List<Header> headers() {
        return headers;
    }
}
