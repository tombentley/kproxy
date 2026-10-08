/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpFactory;
import io.kroxylicious.transform.records.RecordView;
import io.kroxylicious.transform.service.TransformInitializationContext;

@Plugin(configType = Void.class)
public class RecordValue implements OpFactory<RecordView, ByteBuffer> {

    @Override
    public BaseTypedOp<RecordView, ByteBuffer> create(Map<String, Object> config, TransformInitializationContext lookup, Type argumentType) {
        return BaseTypedOp.of(RecordView.class, ByteBuffer.class,
                (record, opContext) -> record.value());
    }
}
