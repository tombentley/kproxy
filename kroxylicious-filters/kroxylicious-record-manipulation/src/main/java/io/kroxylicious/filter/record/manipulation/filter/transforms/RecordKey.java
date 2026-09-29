/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import io.kroxylicious.filter.record.manipulation.filter.transform.BufferTransform;
import io.kroxylicious.filter.record.manipulation.filter.transform.BufferTransformService;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.proxy.plugin.Plugin;

@Plugin(configType = Void.class)
public class RecordKey implements BufferTransformService<Void, Void> {
    @Override
    public Void configure(PluginLookup pluginLookup, Void config) {
        return null;
    }

    @Override
    public BufferTransform create(Void initializationData) {
        return new RecordKeyTransform();
    }
}
