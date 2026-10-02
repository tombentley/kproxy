/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpConfig;
import io.kroxylicious.transform.neo.op.OpConfigs;
import io.kroxylicious.transform.neo.ops.common.OpTypeChecker;
import io.kroxylicious.transform.service.KafkaRecord;
import io.kroxylicious.transform.service.PluginLookup;
import io.kroxylicious.transform.service.TimestampTransform;
import io.kroxylicious.transform.service.TimestampTransformService;

import edu.umd.cs.findbugs.annotations.Nullable;

public class TimestampPipeline implements TimestampTransformService<PipelineConfig, PipelineConfig> {

    @Override
    public PipelineConfig configure(PluginLookup pluginLookup, PipelineConfig config) {
        return config;
    }

    @Override
    public TimestampTransform create(PipelineConfig initializationData) {
        return keyOrValuePipeline(null, initializationData);
    }

    private static TimestampTransform keyOrValuePipeline(
                                                         FilterFactoryContext filterFactoryContext,
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

        var expectedType = Long.class;
        BaseTypedOp<KafkaRecord, ?> compose = OpConfigs.compose(KafkaRecord.class, configs, Set.of(), filterFactoryContext::pluginInstance);
        OpTypeChecker.requireAssignable(expectedType, compose.outputType(),
                "Pipeline configuration has result type " + GenericTypeReflector.getTypeName(compose.outputType())
                        + " and not " + GenericTypeReflector.getTypeName(expectedType) + " as expected");
        return new TimestampTransform() {

            @Nullable
            @Override
            public long apply(KafkaRecord record) {
                return (Long) compose.apply(record, null);
            }
        };
    }

}
