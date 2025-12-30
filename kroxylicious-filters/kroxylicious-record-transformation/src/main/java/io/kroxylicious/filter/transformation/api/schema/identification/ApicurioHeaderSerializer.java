/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import io.kroxylicious.filter.transformation.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.TypeException;

/**
 * The schema identification strategy used by Apicurio Schema Registry: a 9 byte prefix to the data.
 * The first byte is the zero ('magic') byte, followed by an 8 byte identifier.
 */
public class ApicurioHeaderSerializer
        implements OutputSchemaIdentification<ByteWireId> {


    @Override
    public List<Header> prefix(RecordDataLocation site, ByteWireId schemaId, OutputStream outputStream) throws IOException  {
        if (schemaId.bytes().length == 8) {
            // TODO this won't interoperate with the prefix strategy (e.g. headers to prefix transformations).
            return List.of(new RecordHeader("apicurio." + site + ".globalId", schemaId.bytes()));
        }
        return List.of();
    }

    @Override
    public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (type.wireSchemaId() != WireSchemaId.class) {
            throw new TypeException("Not a WireSchemaId: " + type);
        }
        return null;
    }
}
