/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

public interface PropertyStrategy {

    String accessorName(String propertyName);

    String optionalAccessorName(String propertyName);

    String mutatorName(String propertyName);
}
