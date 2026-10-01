/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import io.kroxylicious.proxy.plugin.PluginImplConfig;
import io.kroxylicious.proxy.plugin.PluginImplName;
import io.kroxylicious.transform.service.BufferTransformService;

import edu.umd.cs.findbugs.annotations.Nullable;

public record Then(@Nullable @PluginImplName(BufferTransformService.class) String newTimestamp,
                   @Nullable @PluginImplConfig(implNameProperty = "timestampTransform") Object newTimestampConfig,
                   @Nullable @PluginImplName(BufferTransformService.class) String newKey,
                   @Nullable @PluginImplConfig(implNameProperty = "keyTransform") Object newKeyConfig,
                   @Nullable @PluginImplName(BufferTransformService.class) String newValue,
                   @Nullable @PluginImplConfig(implNameProperty = "valueTransform") Object newValueConfig) {}
