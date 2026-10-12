/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio.api;

public class UnknownGlobalIdException extends SchemaRegistryClientException {

    public UnknownGlobalIdException(GlobalId globalId) {
        super("globalId '%d' is not found.".formatted(globalId.value()));
    }
}
