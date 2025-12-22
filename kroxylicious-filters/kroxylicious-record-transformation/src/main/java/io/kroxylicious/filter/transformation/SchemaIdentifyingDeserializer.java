/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.util.function.Function;

import org.apache.kafka.common.header.Header;

public class SchemaIdentifyingDeserializer<T> implements DatumDeserializer<T> {


    private final String schemaIdHeader = "";
    private Boolean schemaPrefixIs4Bytes = true;
    private final DatumDeserializer<T> deserializer;

    public SchemaIdentifyingDeserializer(DatumDeserializer<T> deserializer) {
        this.deserializer = deserializer;
    }

    @Override
    public Class<T> returnedType() {
        return deserializer.returnedType();
    }

    @Override
    public Datum<T> deserialize(Header[] headers, TransformationInputStream in) throws IOException {
        SchemaIdentifier schemaIdentifier = schemaIdentifier(headers, in);
        Datum<T> datum = deserializer.deserialize(headers, in);
        return new Datum<>(schemaIdentifier, datum.type(), datum.datum());
    }

    private SchemaIdentifier schemaIdentifier(Header[] headers, TransformationInputStream in) throws IOException {
        SchemaIdentifier schemaIdentifier = new NoSchema();
        Long schemaId = null;
        in.mark(9);
        int maybeMagic = in.read();
        if (maybeMagic == 0x00) {
            if (schemaPrefixIs4Bytes != null) {
                if (schemaPrefixIs4Bytes) {
                    schemaId = (long) in.readInt();
                }
                else {
                    schemaId = in.readLong();
                }
                schemaIdentifier = new GlobalId(schemaId);
            }
        }
        else {
            in.reset();
        }
        if (schemaIdHeader != null) {
            schemaIdentifier = extracted(headers, schemaIdHeader);
        }
        else {
            schemaIdentifier = extracted(headers, "io.apicurio.global.id", GlobalId::new);
            if (schemaIdentifier instanceof NoSchema) {
                schemaIdentifier = extracted(headers, "io.apicurio.content.id", ContentId::new);
            }
            if (schemaIdentifier instanceof NoSchema) {
                schemaIdentifier = extracted(headers, "io.apicurio.content.hash", ContentHash::new);
            }
        }
        return schemaIdentifier;
    }

    private SchemaIdentifier extracted(Header[] headers, String headerKey, Function<Long, SchemaIdentifier> fn) throws IOException {
        Long schemaId;
        for (var header : headers) {
            if (header.key().equals(headerKey)) {
                var headerValue = header.value();
                if (schemaPrefixIs4Bytes) {
                    if (headerValue.length == 4) {
                        schemaId = ((long) headerValue[0]) << 24 | ((long) headerValue[1]) << 16 | ((long) headerValue[2]) << 8 | ((long) headerValue[0]);
                    }
                    else if (headerValue.length == 8) {
                        schemaId = ((long) headerValue[0]) << 24 | ((long) headerValue[1]) << 16 | ((long) headerValue[2]) << 8 | ((long) headerValue[0]);
                    }
                    else {
                        throw new IOException("Header with key `" + headerKey + "` had a length of " + headerValue.length + "bytes, which can't be interpreted as a schema id");
                    }
                    return fn.apply(schemaId);
                }
            }
        }
        return new NoSchema();
    }
}
