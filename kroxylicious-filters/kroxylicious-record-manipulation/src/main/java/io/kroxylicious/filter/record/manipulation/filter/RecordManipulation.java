/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.kafka.common.record.Record;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.TypeException;
import io.kroxylicious.filter.record.manipulation.op.OpConfig;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.kafka.PipelineConfig;
import io.kroxylicious.filter.record.manipulation.kafka.RecordTransformConfig;
import io.kroxylicious.proxy.filter.Filter;
import io.kroxylicious.proxy.filter.FilterFactory;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class RecordManipulation implements FilterFactory<RecordManipulationConfig, Init> {

    @Override
    public Init initialize(FilterFactoryContext context,
                           RecordManipulationConfig config) throws PluginConfigurationException {
        RecordTransformConfig recordTransformConfig = config.recordTransform();
        var timestampPipeline = keyOrValuePipeline(context,
                recordTransformConfig.intoTimestamp(),
                new OpConfig(RecordTimestamp.class),
                Long.class);
        var keyPipeline = keyOrValuePipeline(context,
                recordTransformConfig.intoRecordKey(),
                new OpConfig(RecordKey.class),
                ByteBuffer.class);
        var valuePipeline = keyOrValuePipeline(context,
                recordTransformConfig.intoRecordValue(),
                new OpConfig(RecordValue.class),
                ByteBuffer.class);
        return new Init(config.topic(),
                config.direction(),
                timestampPipeline,
                keyPipeline,
                valuePipeline);
    }

    @NonNull
    private static <T> BaseTypedOp<Record, T> keyOrValuePipeline(
            FilterFactoryContext filterFactoryContext,
            @Nullable PipelineConfig keyOrValue,
            OpConfig defaultOrigin,
            Class<T> expectedType) {

        OpConfig first = Optional.ofNullable(keyOrValue).map(x ->
            switch (x.from()) {
                case Timestamp -> new OpConfig(RecordTimestamp.class);
                case RecordValue -> new OpConfig(RecordValue.class);
                case RecordKey -> new OpConfig(RecordKey.class);
            }
        ).orElse(defaultOrigin);

        List<OpConfig> configs = new ArrayList<>();
        configs.add(first);
        if (keyOrValue != null) {
            configs.addAll(keyOrValue.apply());
        }

        BaseTypedOp<Record, ?> compose = OpConfigs.compose(Record.class, configs, Set.of(), filterFactoryContext::pluginInstance);
        if (!GenericTypeReflector.isSuperType(expectedType, compose.outputType())) {
            throw new TypeException("Pipeline configuration has result type " + GenericTypeReflector.getTypeName(compose.outputType())
                    + " and not " + GenericTypeReflector.getTypeName(expectedType) + " as expected");
        }
        return (BaseTypedOp) compose;
    }

    @Override
    public Filter createFilter(FilterFactoryContext context,
                               Init initializationData) {
        return new RecordManipulationFilter(initializationData);
    }
}
