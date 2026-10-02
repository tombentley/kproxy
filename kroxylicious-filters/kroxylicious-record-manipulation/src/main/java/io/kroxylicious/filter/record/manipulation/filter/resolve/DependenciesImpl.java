/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.util.Map;

import io.kroxylicious.transform.service.Dependencies;
import io.kroxylicious.transform.service.DependencyKey;

public class Impl implements Dependencies {
    private final Map<DependencyKey<?, ?>, Map<?, ?>> map;

    Impl(Map<DependencyKey<?, ?>, Map<?, ?>> map) {
        this.map = map;
    }

    public <X, I> void put(DependencyKey<X, I> key, I id, X value) {
        ((Map) this.map).put(key, value);
    }

    @Override
    public <X, I> X get(DependencyKey<X, I> key, I id) {
        return (X) map.get(key).get(id);
    }
}