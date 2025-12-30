/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.format;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public interface DataFormat<S, V> {

    WireSchemaId schemaId();
    Class<V> type();
    Serializer<V> serializer();
    Deserializer<S, V> deserializer();
    // validator validate(TransformationInputStream in)
}
