/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import org.apache.kafka.common.header.Header;

public class SchemaIdHeaderTransformation implements HeadersTransformation {

    @Override
    public Header[] transformHeaders(Header[] headers) {
        return new Header[0]; // TODO
    }
}
