/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.filter.record.manipulation.filter.RecordKey;
import io.kroxylicious.filter.record.manipulation.filter.RecordTimestamp;
import io.kroxylicious.filter.record.manipulation.filter.RecordValue;
import io.kroxylicious.filter.record.manipulation.filter.transform.BufferTransform;
import io.kroxylicious.filter.record.manipulation.filter.transform.BufferTransformService;
import io.kroxylicious.filter.record.manipulation.kafka.PipelineConfig;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpConfig;
import io.kroxylicious.filter.record.manipulation.op.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.ops.common.OpTypeChecker;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.proxy.plugin.Plugin;

import edu.umd.cs.findbugs.annotations.Nullable;

@Plugin(configType = PipelineConfig.class)
public class BufferPipeline implements BufferTransformService<PipelineConfig, BaseTypedOp<Record, ?>> {

    @Override
    public BaseTypedOp<Record, ?> configure(PluginLookup pluginLookup, PipelineConfig config) {
        return keyOrValuePipeline(pluginLookup, config);
    }

    @Override
    public BufferTransform create(BaseTypedOp<Record, ?> compose) {
        return new BufferTransform() {

            @Nullable
            @Override
            public ByteBuffer apply(Record record) {
                return (ByteBuffer) compose.apply(record, null);
            }
        };
    }

    private static BaseTypedOp<Record, ?> keyOrValuePipeline(
                                                             PluginLookup pluginLookup,
                                                             PipelineConfig keyOrValue) {

        OpConfig first = switch (keyOrValue.from()) {
            case Timestamp -> new OpConfig(RecordTimestamp.class);
            case RecordValue -> new OpConfig(RecordValue.class);
            case RecordKey -> new OpConfig(RecordKey.class);
        };

        List<OpConfig> configs = new ArrayList<>();
        configs.add(first);
        if (keyOrValue != null) {
            configs.addAll(keyOrValue.apply());
        }

        var expectedType = ByteBuffer.class;
        BaseTypedOp<Record, ?> compose = OpConfigs.compose(Record.class, configs, Set.of(), pluginLookup);
        OpTypeChecker.requireAssignable(expectedType, compose.outputType(),
                "Pipeline configuration has result type " + GenericTypeReflector.getTypeName(compose.outputType())
                        + " and not " + GenericTypeReflector.getTypeName(expectedType) + " as expected");
        return compose;
    }

}
