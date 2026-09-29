/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.service.PluginLookup;
import io.kroxylicious.transform.service.TimestampTransform;
import io.kroxylicious.transform.service.TimestampTransformService;

@Plugin(configType = Void.class)
public class RecordTimestamp implements TimestampTransformService<Void, Void> {
    @Override
    public Void configure(PluginLookup pluginLookup, Void config) {
        return null;
    }

    @Override
    public TimestampTransform create(Void initializationData) {
        return new RecordTimestampTransform();
    }

    @Override
    public void close(Void initializationData) {

    }
}
