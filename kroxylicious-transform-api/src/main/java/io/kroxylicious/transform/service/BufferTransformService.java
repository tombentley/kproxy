/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.service;

public interface BufferTransformService<C, I> {

    I configure(PluginLookup pluginLookup, C config);

    BufferTransform create(I initializationData);

    default void close(I initializationData) {
    }
}
