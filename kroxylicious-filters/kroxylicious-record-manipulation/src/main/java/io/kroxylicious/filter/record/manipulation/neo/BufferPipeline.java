/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.neo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.filter.record.manipulation.neo.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.neo.op.OpConfig;
import io.kroxylicious.filter.record.manipulation.neo.op.OpConfigs;
import io.kroxylicious.filter.record.manipulation.neo.ops.common.OpTypeChecker;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.service.BufferTransform;
import io.kroxylicious.transform.service.BufferTransformService;
import io.kroxylicious.transform.service.PluginLookup;

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
