/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import io.kroxylicious.proxy.plugin.PluginImplConfig;
import io.kroxylicious.proxy.plugin.PluginImplName;
import io.kroxylicious.transform.service.BufferTransformService;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The filter configuration.
 * @param topic The topic
 * @param direction The direction
 * @param timestampTransform The FQCN of the timestamp transform
 * @param timestampTransformConfig The configuration for the timestamp transform
 * @param keyTransform The FQCN of the key transform
 * @param keyTransformConfig The configuration for the key transform
 * @param valueTransform The FQCN of the value transform
 * @param valueTransformConfig The configuration for the value transform
 */
public record RecordManipulationConfig(
                                       String topic,
                                       Direction direction,
                                       @Nullable @PluginImplName(BufferTransformService.class) String timestampTransform,
                                       @Nullable @PluginImplConfig(implNameProperty = "timestampTransform") Object timestampTransformConfig,
                                       @Nullable @PluginImplName(BufferTransformService.class) String keyTransform,
                                       @Nullable @PluginImplConfig(implNameProperty = "keyTransform") Object keyTransformConfig,
                                       @Nullable @PluginImplName(BufferTransformService.class) String valueTransform,
                                       @Nullable @PluginImplConfig(implNameProperty = "valueTransform") Object valueTransformConfig) {}
