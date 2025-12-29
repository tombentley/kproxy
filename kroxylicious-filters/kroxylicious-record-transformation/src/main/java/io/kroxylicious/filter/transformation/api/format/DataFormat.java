/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.format;

public interface DataFormat<T> {

    Class<T> type();
    Serializer<T> serializer();
    Deserializer<T> deserializer();
    // validator validate(TransformationInputStream in)
}
