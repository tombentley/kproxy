/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.model;

public class VisitorException extends RuntimeException {
    public VisitorException(
                          String message,
                          Throwable cause) {
        super(message, cause);
    }
}
