/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

public interface SchemaTransformation {
    static SchemaTransformation preserve() {
        return schemaIdentifier -> schemaIdentifier;
    }
    static SchemaTransformation drop() {
        return schemaIdentifier -> NoSchema.INSTANCE;
    }
    static SchemaTransformation globalId(long globalId) {
        return schemaIdentifier -> new GlobalId(globalId);
    }
    static SchemaTransformation contentId(long contentId) {
        return schemaIdentifier -> new ContentId(contentId);
    }
    SchemaIdentifier schemaIdentifier(SchemaIdentifier schemaIdentifier);
}


