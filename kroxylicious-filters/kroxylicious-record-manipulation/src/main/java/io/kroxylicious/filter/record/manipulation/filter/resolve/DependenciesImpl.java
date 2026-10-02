/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.util.HashMap;
import java.util.Map;

import io.kroxylicious.transform.service.Dependencies;
import io.kroxylicious.transform.service.DependencyKey;

public class DependenciesImpl implements Dependencies {
    private final Map<DependencyKey<?, ?>, Map<?, ?>> map;

    DependenciesImpl() {
        this.map = new HashMap<>();
    }

    public <I, V> void put(DependencyKey<I, V> key, Map<I, V> values) {
        ((Map) this.map).put(key, values);
    }

    @Override
    public <I, V> V get(DependencyKey<I, V> key, I id) {
        return (V) map.get(key).get(id);
    }
}