/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.OutputStream;
import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.TypeException;

public class NoSchemaSerializer implements OutputSchemaIdentification<NoSchema> {

    public static final NoSchemaSerializer INSTANCE = new NoSchemaSerializer();

    @Override
    public List<Header> prefix(RecordDataLocation site, NoSchema schemaId, OutputStream outputStream) {
        return List.of();
    }

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (type.wireSchemaId() != NoSchema.class) {
            throw new TypeException("Not a NoSchema: " + type);
        }
        return null;
    }
}
