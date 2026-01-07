/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.model;

import io.kroxylicious.filter.transformation.api.format.DataFormat;

public record RecordFormat(
        DataFormat keyFormat,
        DataFormat valueFormat) {
}
