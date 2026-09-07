/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import java.util.List;

import io.kroxylicious.filter.record.manipulation.config.OpConfig;

import edu.umd.cs.findbugs.annotations.Nullable;

public record PipelineConfig(
        Origin from,
        @Nullable List<OpConfig> apply) {
}
