/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import edu.umd.cs.findbugs.annotations.Nullable;

public record RecordWhen(@Nullable SchemaRef withKeySchema,
                         @Nullable SchemaRef withValueSchema,
                         @Nullable HeaderPredicate hasAnyHeader) {}
