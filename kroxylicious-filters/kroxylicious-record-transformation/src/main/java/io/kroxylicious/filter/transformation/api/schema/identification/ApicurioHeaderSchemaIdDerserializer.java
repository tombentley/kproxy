/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.TransformationInputStream;
import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.mapper.Context;

/**
 * The schema identification strategy used by Apicurio Schema Registry: a 9 byte prefix to the data.
 * The first byte is the zero ('magic') byte, followed by an 8 byte identifier.
 */
public class ApicurioHeaderSchemaIdDerserializer
        implements SchemaIdDeserializer<ApicurioSchemaCoordinates> {

    @Override
    public Type<ApicurioSchemaCoordinates, ?, ?> typeCheck(Type<?, ?, ?> type) {
        if (!TransformationInputStream.class.isAssignableFrom(type.cls())) {
            throw new TypeException(String.format("Type %s is not assignable to InputStream", type));
        }
        return new Type<>(ApicurioSchemaCoordinates.class, Void.class, TransformationInputStream.class);
    }

    @Override
    public SchemaAndValue<Void, InputStream> deserialize(InputStream data, Context context) {
        byte[] globalId = null;
        String groupId = null;
        String artifactId = null;
        String version = null;
        for (Header header : context.headers()) {
            var key = header.key();
            if (key.equals("apicurio." + context.location() + ".globalId")) {
                if (globalId == null) {
                    if (header.value().length == 8) {
                        globalId = header.value();
                    }
                }
            }
            else if (key.equals("apicurio." + context.location() + ".groupId")) {
                if (groupId == null) {
                    groupId = new String(header.value(), StandardCharsets.UTF_8);
                }
            }
            else if (key.equals("apicurio." + context.location() + ".artifactId")) {
                if (artifactId == null) {
                    artifactId = new String(header.value(), StandardCharsets.UTF_8);
                }
            }
            else if (key.equals("apicurio." + context.location() + ".version")) {
                if (version == null) {
                    version = new String(header.value(), StandardCharsets.UTF_8);
                }
            }
        }
        return new SchemaAndValue<>(new ApicurioSchemaCoordinates(globalId, groupId, artifactId, version), null, data);
    }

}
