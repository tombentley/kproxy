/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import java.time.Duration;
import java.time.ZonedDateTime;

import io.kroxylicious.kafka.common.record.internal.CompressionType;

import edu.umd.cs.findbugs.annotations.Nullable;

public record Then(@Nullable CompressionType compressionType,
                   @Nullable ZonedDateTime newTimestamp,
                   @Nullable Duration shiftTimestampBy,
                   @Nullable RecordWhen ifRecord,
                   RecordThen thenNewRecord) {
    public Then {
        if (newTimestamp != null && shiftTimestampBy != null) {
            throw new IllegalArgumentException("At most one of `newTimestamp` and `shiftTimestampBy` can be specified");
        }
    }
}
