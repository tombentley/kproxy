/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import io.kroxylicious.transform.service.BufferTransform;
import io.kroxylicious.transform.service.BufferTransformService;
import io.kroxylicious.transform.service.TimestampTransform;
import io.kroxylicious.transform.service.TimestampTransformService;

public record Init(String topic,
                   Direction direction,
                   TimestampTransformService timestampTransformService,
                   Object timestampInitData,
                   TimestampTransform recordTimestampPipeline,
                   BufferTransformService keyTransformService,
                   Object keyInitData,
                   BufferTransform recordKeyPipeline,
                   BufferTransformService valueTransformService,
                   Object valueInitData,
                   BufferTransform recordValuePipeline) {}
