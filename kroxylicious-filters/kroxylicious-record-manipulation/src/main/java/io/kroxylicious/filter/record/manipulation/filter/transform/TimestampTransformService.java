/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transform;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;

public interface TimestampTransformService<C, I> {

    I configure(PluginLookup pluginLookup, C config);

    TimestampTransform create(I initializationData);

    default void close(I initializationData) {
    }
}
