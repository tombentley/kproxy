/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.kafka.common.header.Header;

import edu.umd.cs.findbugs.annotations.Nullable;

public class SchemaIdentifyingDeserializer<T> implements DatumDeserializer<T> {

    private final @Nullable String globalIdHeaderName;
    private final @Nullable String contentIdHeaderName;
    private final @Nullable String contentHashHeaderName;
    private final boolean schemaPrefixIs4Bytes;
    private final DatumDeserializer<T> deserializer;

    public SchemaIdentifyingDeserializer(String contentHashHeaderName,
                                         String globalIdHeaderName,
                                         String contentIdHeaderName,
                                         Boolean schemaPrefixIs4Bytes,
                                         DatumDeserializer<T> deserializer) {
        this.contentHashHeaderName = contentHashHeaderName;
        this.globalIdHeaderName = globalIdHeaderName;
        this.contentIdHeaderName = contentIdHeaderName;
        this.schemaPrefixIs4Bytes = schemaPrefixIs4Bytes;
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
        SchemaIdentifier schemaIdentifier = NoSchema.INSTANCE;
        schemaIdentifier = fromPrefix(in, schemaIdentifier);
        if (schemaIdentifier instanceof NoSchema && globalIdHeaderName != null) {
            schemaIdentifier = extracted(headers, globalIdHeaderName, parseLong().andThen(GlobalId::new));
        }
        if (schemaIdentifier instanceof NoSchema && contentIdHeaderName != null) {
            schemaIdentifier = extracted(headers, contentIdHeaderName, parseLong().andThen(ContentId::new));
        }
        if (schemaIdentifier instanceof NoSchema && contentHashHeaderName != null) {
            schemaIdentifier = extracted(headers, contentHashHeaderName, (x, y) -> new ContentHash(y));
        }
        return schemaIdentifier;
    }

    private SchemaIdentifier fromPrefix(TransformationInputStream in, SchemaIdentifier schemaIdentifier) throws IOException {
        in.mark(schemaPrefixIs4Bytes ? 5 : 9);
        int maybeMagic = in.read();
        if (maybeMagic == 0x00) {
            if (schemaPrefixIs4Bytes && in.available() >= 4) {
                schemaIdentifier = new Prefix(in.readNBytes(4));
            }
            else if (!schemaPrefixIs4Bytes && in.available() >= 8) {
                schemaIdentifier = new Prefix(in.readNBytes(8));
            }
            else {
                in.reset();
            }
        }
        else {
            in.reset();
        }
        return schemaIdentifier;
    }

    private SchemaIdentifier extracted(Header[] headers, String headerKey, BiFunction<String, byte[], SchemaIdentifier> fn) {
        return firstHeaderWithKey(headers, headerKey)
                .map(headerValue -> fn.apply(headerKey, headerValue))
                .orElse(NoSchema.INSTANCE);
    }

    BiFunction<String, byte[], Long> parseLong() {
        return this::parseLong;
    }

    long parseLong(String headerKey, byte[] headerValue) {
        long schemaId;
        if (schemaPrefixIs4Bytes && headerValue.length == 4) {
            schemaId = ((long) headerValue[0]) << 24 | ((long) headerValue[1]) << 16 | ((long) headerValue[2]) << 8 | ((long) headerValue[0]);
        }
        else if (!schemaPrefixIs4Bytes && headerValue.length == 8) {
            schemaId = ((long) headerValue[0]) << 24 | ((long) headerValue[1]) << 16 | ((long) headerValue[2]) << 8 | ((long) headerValue[0]);
        }
        else {
            throw new RuntimeException("Header with key `" + headerKey + "` had a length of " + headerValue.length + " bytes, which can't be interpreted as a schema id");
        }
        return schemaId;
    }

    private static Optional<byte[]> firstHeaderWithKey(Header[] headers, String headerKey) {
        for (var header : headers) {
            if (header.key().equals(headerKey)) {
                return Optional.of(header.value());
            }
        }
        return Optional.empty();
    }
}
