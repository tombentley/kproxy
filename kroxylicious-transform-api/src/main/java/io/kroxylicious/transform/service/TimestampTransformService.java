/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.service;

public interface TimestampTransformService<C, I> {

    I configure(TransformInitializationContext pluginLookup, C config);

    TimestampTransform create(I initializationData);

    default void close(I initializationData) {
    }
}
