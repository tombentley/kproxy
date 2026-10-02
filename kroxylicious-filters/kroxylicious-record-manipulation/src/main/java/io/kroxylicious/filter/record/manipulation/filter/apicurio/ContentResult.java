/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio;

import edu.umd.cs.findbugs.annotations.Nullable;

// ContentResult carries raw schema bytes, so an array component is intentional here.
@SuppressWarnings("ArrayRecordComponent")
public record ContentResult(ContentId contentId, @Nullable String type, byte[] contentBytes) {}
