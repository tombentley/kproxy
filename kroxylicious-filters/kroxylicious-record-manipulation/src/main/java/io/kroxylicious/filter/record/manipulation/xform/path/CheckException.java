/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

/**
 * Thrown when expression does not pass the type checker.
 */
public class CheckException extends RuntimeException {
    public CheckException(String message) {
        super(message);
    }
}
