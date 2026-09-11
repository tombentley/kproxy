/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.ops.jsonschema.networknt.jackson3;

public class SchemaNotValidException extends RuntimeException {
    public SchemaNotValidException() {
    }

    public SchemaNotValidException(String message) {
        super(message);
    }

    public SchemaNotValidException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchemaNotValidException(Throwable cause) {
        super(cause);
    }
}
