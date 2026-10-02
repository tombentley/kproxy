/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.service;

public final class DependencyKey<X, I> {
    private final Class<X> cls;

    public DependencyKey(Class<X> cls, Class<I> idClass) {
        this.cls = cls;
    }
}
