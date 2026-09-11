/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.lang.reflect.Type;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.proxy.plugin.Plugin;

@Plugin(configType = Void.class)
public class RecordTimestamp implements OpFactory<Record, Long> {

    @Override
    public BaseTypedOp<Record, Long> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        return BaseTypedOp.of(Record.class, Long.class,
                (record, opContext) -> record.timestamp());
    }
}
