/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.service.BufferTransform;
import io.kroxylicious.transform.service.BufferTransformService;
import io.kroxylicious.transform.service.TransformInitializationContext;

@Plugin(configType = Void.class)
public class NullBuffer implements BufferTransformService<Void, Void> {
    @Override
    public Void configure(Void config, TransformInitializationContext context) {
        return null;
    }

    @Override
    public BufferTransform create(Void initializationData) {
        return new NullBufferTransform();
    }
}
