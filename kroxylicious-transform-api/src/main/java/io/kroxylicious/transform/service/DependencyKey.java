/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.service;

/**
 * A dependency
 * @param <I> The type of Id
 * @param <V> The type of the depended-upon value
 */
public final class DependencyKey<I, V> {
    private final Class<V> valueClass;

    public DependencyKey(Class<I> idClass, Class<V> valueClass) {
        this.valueClass = valueClass;
    }
}
