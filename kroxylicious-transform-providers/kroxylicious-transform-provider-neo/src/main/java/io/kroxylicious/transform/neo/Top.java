/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo;

import java.util.Map;

import io.kroxylicious.transform.neo.op.OpConfig;

public record Top(
                  RecordTransformConfig2 result,
                  Map<String, OpConfig> where) {}
