/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.IOException;
import java.util.List;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import io.kroxylicious.filter.transformation.RecordDataLocation;
import io.kroxylicious.filter.transformation.TransformationInputStream;

/**
 * The schema identification strategy used by Apicurio Schema Registry: a 9 byte prefix to the data.
 * The first byte is the zero ('magic') byte, followed by an 8 byte identifier.
 */
public class ApicurioHeaderSchemaIdentificationStrategy
        implements InputSchemaIdentification<ApicurioSchemaCoordinates>,
        OutputSchemaIdentification<ByteWireId> {

    @Override
    public ApicurioSchemaCoordinates schemaIdFromData(List<Header> headers, RecordDataLocation site, TransformationInputStream data) throws IOException {
        return headers.stream()
                .filter(header -> header.key().equals("apicurio." + site + ".globalId"))
                .findFirst()
                .<WireSchemaId>map(header ->
                        // TODO should validate that it's 4 bytes
                        new ByteWireId(header.value())
                )
                .orElse(null);

    }

    @Override
    public byte[] prefix(ByteWireId schemaId) {
        return new byte[0];
    }

    @Override
    public List<Header> headers(ByteWireId schemaId, RecordDataLocation site) {
        if (schemaId.bytes().length == 8) {
            // TODO this won't interoperate with the prefix strategy (e.g. headers to prefix transformations).
            return List.of(new RecordHeader("apicurio." + site + ".globalId", schemaId.bytes()));
        }
        return List.of();
    }

    @Override
    public Class<ApicurioSchemaCoordinates> returnedType() {
        return ApicurioSchemaCoordinates.class;
    }

    @Override
    public Class<ByteWireId> acceptedType() {
        return ByteWireId.class;
    }
}
