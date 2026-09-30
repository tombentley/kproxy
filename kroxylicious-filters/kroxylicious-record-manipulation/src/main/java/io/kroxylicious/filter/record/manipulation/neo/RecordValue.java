/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.neo;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

import io.kroxylicious.filter.record.manipulation.neo.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.neo.op.OpFactory;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.service.PluginLookup;

@Plugin(configType = Void.class)
public class RecordValue implements OpFactory<Record, ByteBuffer> {

    @Override
    public BaseTypedOp<Record, ByteBuffer> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        return BaseTypedOp.of(Record.class, ByteBuffer.class,
                (record, opContext) -> record.value());
    }
}
