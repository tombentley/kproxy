/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;

import org.apache.kafka.common.header.Header;

public interface DatumDeserializer<T> {

    Class<T> returnedType();

    Datum<T> deserialize(Header[] headers, TransformationInputStream in) throws IOException;
}
