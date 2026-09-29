/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import io.kroxylicious.filter.record.manipulation.filter.transform.BufferTransform;
import io.kroxylicious.filter.record.manipulation.filter.transform.BufferTransformService;
import io.kroxylicious.filter.record.manipulation.filter.transform.TimestampTransform;

public record Init(String topic,
                   Direction direction,
                   io.kroxylicious.filter.record.manipulation.filter.transform.TimestampTransformService timestampTransformService, Object timestampInitData,
                   TimestampTransform recordTimestampPipeline,
                   BufferTransformService keyTransformService, Object keyInitData, BufferTransform recordKeyPipeline,
                   BufferTransformService valueTransformService, Object valueInitData, BufferTransform recordValuePipeline) {}
