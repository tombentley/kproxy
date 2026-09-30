/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.util.Optional;

import io.kroxylicious.filter.record.manipulation.filter.transforms.RecordKey;
import io.kroxylicious.filter.record.manipulation.filter.transforms.RecordTimestamp;
import io.kroxylicious.filter.record.manipulation.filter.transforms.RecordValue;
import io.kroxylicious.proxy.filter.Filter;
import io.kroxylicious.proxy.filter.FilterFactory;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;
import io.kroxylicious.transform.service.BufferTransformService;
import io.kroxylicious.transform.service.TimestampTransformService;

/**
 * The FilterFactory (service) for the {@link RecordManipulationFilter}.
 */
@Plugin(configType = RecordManipulationConfig.class)
public class RecordManipulation implements FilterFactory<RecordManipulationConfig, Init> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Init initialize(FilterFactoryContext context,
                           RecordManipulationConfig config)
            throws PluginConfigurationException {

        TimestampTransformService timestampTransformService = context.pluginInstance(TimestampTransformService.class,
                Optional.ofNullable(config.timestampTransform()).orElse(RecordTimestamp.class.getName()));
        var timestampInitData = timestampTransformService
                .configure(context::pluginInstance, config.timestampTransformConfig());
        var timestampPipeline = timestampTransformService.create(timestampInitData);

        BufferTransformService keyTransformService = context.pluginInstance(BufferTransformService.class,
                Optional.ofNullable(config.keyTransform()).orElse(RecordKey.class.getName()));
        var keyInitData = keyTransformService
                .configure(context::pluginInstance, config.keyTransformConfig());
        var keyPipeline = keyTransformService.create(keyInitData);

        BufferTransformService valueTransformService = context.pluginInstance(BufferTransformService.class,
                Optional.ofNullable(config.valueTransform()).orElse(RecordValue.class.getName()));
        var valueInitData = valueTransformService
                .configure(context::pluginInstance, config.valueTransformConfig());
        var valuePipeline = valueTransformService.create(valueInitData);

        return new Init(config.topic(),
                config.direction(),
                timestampTransformService,
                timestampInitData,
                timestampPipeline,

                keyTransformService,
                keyInitData,
                keyPipeline,

                valueTransformService,
                valueInitData,
                valuePipeline);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void close(Init initializationData) {
        try {
            ((TimestampTransformService) initializationData.timestampTransformService()).close(initializationData.timestampInitData());
        }
        finally {
            try {
                ((BufferTransformService) initializationData.keyTransformService()).close(initializationData.keyInitData());
            }
            finally {
                ((BufferTransformService) initializationData.valueTransformService()).close(initializationData.valueInitData());
            }
        }
    }

    @Override
    public Filter createFilter(FilterFactoryContext context,
                               Init initializationData) {
        return new RecordManipulationFilter(initializationData, context.filterDispatchExecutor(), null);
    }
}
