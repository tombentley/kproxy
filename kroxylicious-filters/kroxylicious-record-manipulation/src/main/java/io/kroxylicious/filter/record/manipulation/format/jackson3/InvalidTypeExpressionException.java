/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson3;

import java.util.List;

public class InvalidTypeExpressionException extends RuntimeException {
    public InvalidTypeExpressionException(String string, List<String> strings) {
        super(string);
    }
}
