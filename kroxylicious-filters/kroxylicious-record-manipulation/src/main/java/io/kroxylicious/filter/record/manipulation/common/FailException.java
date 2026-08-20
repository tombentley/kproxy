/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

/**
 * A failed assertion
 */
public class FailException extends RuntimeException {
    public FailException(String message) {
        super(message);
    }
}
