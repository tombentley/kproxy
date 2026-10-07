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

/**
 *           withTransformedHeaders:
 *             removeAllHeaders:
 *               withHeadersKey: my-header
 *             addHeaderLast:
 *               key: your-header
 *               value:
 *                 base64: e45iugh5e9468g8653
 *           withKey:
 *             value:
 *               base64: e45iugh5e9468g8653
 *           withKey:
 *             transformedBy:
 *               type: NeoPipe
 *               transforms:
 *                 - transform: ReadJson
 *                   config:
 *                 - transform: JsonSchemaValidation
 *                   config:
 *                 - transform: Jsonata
 *                   jsonata: |
 *                 - transform: WriteJson
 *                   config:
 *           withTransformedValue:
 *             reader: JsonRead # only needed
 *             readerConfig:
 *             transforms:
 *               - transform: Jsonata
 *                 jsonata: |
 *             writer: WriteJson
 *             writerConfig:
 * @param newKey The new key
 * @param newKeyConfig The new key config
 * @param newValue The new value
 * @param newValueConfig The new value config
 */
public record RecordThen(@Nullable @PluginImplName(BufferTransformService.class) String newKey,
                         @Nullable @PluginImplConfig(implNameProperty = "keyTransform") Object newKeyConfig,
                         @Nullable @PluginImplName(BufferTransformService.class) String newValue,
                         @Nullable @PluginImplConfig(implNameProperty = "valueTransform") Object newValueConfig) {}
