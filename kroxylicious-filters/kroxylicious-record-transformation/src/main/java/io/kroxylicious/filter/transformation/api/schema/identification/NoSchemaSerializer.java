/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.RecordDataLocation;

public class NoSchemaSerializer implements OutputSchemaIdentification<NoSchema> {

    public static final NoSchemaSerializer INSTANCE = new NoSchemaSerializer();

    @Override
    public Class<NoSchema> acceptedType() {
        return NoSchema.class;
    }

    @Override
    public byte[] prefix(NoSchema schemaId) {
        return new byte[0];
    }

    @Override
    public List<Header> headers(NoSchema schemaId, RecordDataLocation site) {
        return List.of();
    }
}
