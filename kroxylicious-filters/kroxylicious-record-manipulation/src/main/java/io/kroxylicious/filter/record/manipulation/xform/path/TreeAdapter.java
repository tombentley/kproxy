/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.Map;

public interface TreeAdapter<N> {
    boolean isObject(N node);
    boolean isArray(N node);
    Iterable<? extends Map.Entry<String, N>> objectProperties(N node);
    int arrayLength(N node);
    N arrayItem(N node, int index);
}
