/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio.api;

public class ContentNotFoundException extends SchemaRegistryClientException {

    public ContentNotFoundException(Record contentId) {
        super("content '%s' is not found.".formatted(contentId));
    }
}
