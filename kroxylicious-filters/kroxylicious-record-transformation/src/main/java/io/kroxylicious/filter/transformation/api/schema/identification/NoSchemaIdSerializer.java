/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.OutputStream;
import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.api.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.TypeException;

public class NoSchemaIdSerializer implements SchemaIdSerializer<NoSchemaId> {

    public static final NoSchemaIdSerializer INSTANCE = new NoSchemaIdSerializer();

    @Override
    public List<Header> serializeSchemaId(RecordDataLocation site, NoSchemaId schemaId, OutputStream outputStream) {
        return List.of();
    }

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (type.wireSchemaId() != NoSchemaId.class) {
            throw new TypeException("Not a NoSchema: " + type);
        }
        return null;
    }
}
