/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.kafka;

import java.util.List;

/**
 * Thrown when expression could not be parsed.
 */
public class InvalidExpressionException extends RuntimeException {
    public InvalidExpressionException(String message, List<String> strings) {
        super(message);
    }
}
