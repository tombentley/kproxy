/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio.api;

public class SchemaRegistryClientException extends RuntimeException {

    public SchemaRegistryClientException(String message) {
        super(message);
    }

    public SchemaRegistryClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
