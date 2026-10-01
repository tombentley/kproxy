/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio;

import io.kroxylicious.filter.record.manipulation.filter.ContentId;

class UnknownContentIdException extends SchemaRegistryClientException {

    UnknownContentIdException(ContentId contentId) {
        super("contentId '%d' is not found.".formatted(contentId.value()));
    }
}
