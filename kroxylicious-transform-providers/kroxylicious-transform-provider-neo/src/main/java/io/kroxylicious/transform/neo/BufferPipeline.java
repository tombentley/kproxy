/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpConfig;
import io.kroxylicious.transform.neo.op.OpConfigs;
import io.kroxylicious.transform.neo.ops.common.OpTypeChecker;
import io.kroxylicious.transform.records.RecordView;
import io.kroxylicious.transform.service.BufferTransform;
import io.kroxylicious.transform.service.BufferTransformService;
import io.kroxylicious.transform.service.TransformInitializationContext;

import edu.umd.cs.findbugs.annotations.Nullable;

@Plugin(configType = PipelineConfig.class)
public class BufferPipeline implements BufferTransformService<PipelineConfig, BaseTypedOp<RecordView, ?>> {

    @Override
    public BaseTypedOp<RecordView, ?> configure(PipelineConfig config, TransformInitializationContext context) {
        return keyOrValuePipeline(config, context);
    }

    @Override
    public BufferTransform create(BaseTypedOp<RecordView, ?> compose) {
        return new BufferTransform() {

            @Nullable
            @Override
            public CompletionStage<ByteBuffer> apply(RecordView record) {
                return (CompletionStage) compose.apply(record, null);
            }
        };
    }

    private static BaseTypedOp<RecordView, ?> keyOrValuePipeline(
                                                                 PipelineConfig keyOrValue,
                                                                 TransformInitializationContext context) {

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
        BaseTypedOp<RecordView, ?> compose = OpConfigs.compose(RecordView.class, configs, context);
        OpTypeChecker.requireAssignable(expectedType, compose.outputType(),
                "Pipeline configuration has result type " + GenericTypeReflector.getTypeName(compose.outputType())
                        + " and not " + GenericTypeReflector.getTypeName(expectedType) + " as expected");
        return compose;
    }

}
