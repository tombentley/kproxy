/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import edu.umd.cs.findbugs.annotations.Nullable;

public interface DeserializerFactory {

    @Nullable
    Deser<?> deser(Format<?> format);
}
