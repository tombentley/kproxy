/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.io.InputStream;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.api.format.Deserializer;

import edu.umd.cs.findbugs.annotations.Nullable;

public class SchemaIdentifyingDeserializer<T> implements Deserializer<T> {

    private final @Nullable String globalIdHeaderName;
    private final @Nullable String contentIdHeaderName;
    private final @Nullable String contentHashHeaderName;
    private final boolean schemaPrefixIs4Bytes;
    private final Deserializer<T> deserializer;

    public SchemaIdentifyingDeserializer(String contentHashHeaderName,
                                         String globalIdHeaderName,
                                         String contentIdHeaderName,
                                         Boolean schemaPrefixIs4Bytes,
                                         Deserializer<T> deserializer) {
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
    public T deserialize(Header[] headers, InputStream in) throws IOException {
        T value = deserializer.deserialize(headers, in);
        return value;
    }
/*
    private WireSchemaId schemaIdentifier(Header[] headers, TransformationInputStream in) throws IOException {
        WireSchemaId wireSchemaId = NoSchema.INSTANCE;
        wireSchemaId = fromPrefix(in, wireSchemaId);
        if (wireSchemaId instanceof NoSchema && globalIdHeaderName != null) {
            wireSchemaId = extracted(headers, globalIdHeaderName, parseLong().andThen(GlobalId::new));
        }
        if (wireSchemaId instanceof NoSchema && contentIdHeaderName != null) {
            wireSchemaId = extracted(headers, contentIdHeaderName, parseLong().andThen(ContentId::new));
        }
        if (wireSchemaId instanceof NoSchema && contentHashHeaderName != null) {
            wireSchemaId = extracted(headers, contentHashHeaderName, (x, y) -> new ContentHash(y));
        }
        return wireSchemaId;
    }

    private WireSchemaId fromPrefix(TransformationInputStream in, WireSchemaId wireSchemaId) throws IOException {
        in.mark(schemaPrefixIs4Bytes ? 5 : 9);
        int maybeMagic = in.read();
        if (maybeMagic == 0x00) {
            if (schemaPrefixIs4Bytes && in.available() >= 4) {
                wireSchemaId = new Prefix(in.readNBytes(4));
            }
            else if (!schemaPrefixIs4Bytes && in.available() >= 8) {
                wireSchemaId = new Prefix(in.readNBytes(8));
            }
            else {
                in.reset();
            }
        }
        else {
            in.reset();
        }
        return wireSchemaId;
    }

    private WireSchemaId extracted(Header[] headers, String headerKey, BiFunction<String, byte[], WireSchemaId> fn) {
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
 */
}
