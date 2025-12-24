/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import io.kroxylicious.filter.transformation.RecordDataLocation;

public interface SchemaTransformation {
    static SchemaTransformation preserve() {
        return (topicName, dataLocation, wireSchemaId) -> wireSchemaId;
    }
    static SchemaTransformation drop() {
        return (topicName, dataLocation, wireSchemaId) -> NoSchema.INSTANCE;
    }
    WireSchemaId schemaIdentifier(String topicName,
                                  RecordDataLocation dataLocation,
                                  WireSchemaId wireSchemaId);
}


