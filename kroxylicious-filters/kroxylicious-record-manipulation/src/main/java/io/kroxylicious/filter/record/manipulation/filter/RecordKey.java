/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.kafka.common.record.Record;

import io.kroxylicious.filter.record.manipulation.common.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.common.OpFactory;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;

public class RecordKey implements OpFactory<Record, ByteBuffer> {

    @Override
    public BaseTypedOp<Record, ByteBuffer> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        return BaseTypedOp.of(Record.class, ByteBuffer.class,
                (record, opContext) -> record.key());
    }
}
