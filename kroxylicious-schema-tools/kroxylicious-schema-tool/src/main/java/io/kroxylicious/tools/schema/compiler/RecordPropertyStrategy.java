/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

public class RecordPropertyStrategy implements PropertyStrategy {
    @Override
    public String accessorName(
                               String propertyName) {
        return CodeGen.fieldName(propertyName);
    }

    @Override
    public String optionalAccessorName(String propertyName) {
        return "opt_" + accessorName(propertyName);
    }

    @Override
    public String mutatorName(
                              String propertyName) {
        return CodeGen.fieldName(propertyName);
    }
}
