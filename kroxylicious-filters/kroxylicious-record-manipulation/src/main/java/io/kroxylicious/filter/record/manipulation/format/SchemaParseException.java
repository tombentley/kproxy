/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format;

public class SchemaParseException extends RuntimeException {
    public SchemaParseException() {
    }

    public SchemaParseException(Throwable cause) {
        super(cause);
    }

    public SchemaParseException(String message) {
        super(message);
    }

    public SchemaParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
