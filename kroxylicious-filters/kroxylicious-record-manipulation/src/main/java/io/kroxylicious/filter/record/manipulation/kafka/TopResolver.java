/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import java.lang.reflect.Type;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;

public class TopResolver {

    void resolve(Top top, PluginLookup lookup) {
        top.result().headers();
        top.where().forEach((name, opConfig) -> {
            OpFactory<?, ?> factory = lookup.pluginInstance(OpFactory.class, opConfig.op());
            Type argumentType = null; // TODO
            BaseTypedOp<?, ?> op = factory.create(opConfig.config(), lookup, argumentType);
        });
    }
}
