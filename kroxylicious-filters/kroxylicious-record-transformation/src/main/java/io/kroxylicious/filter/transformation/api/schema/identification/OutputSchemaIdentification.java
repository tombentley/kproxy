/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.RecordDataLocation;

/**
 * How schemas are identified during serialization.
 */
public interface OutputSchemaIdentification<W extends WireSchemaId> {

    Class<W> acceptedType();

    /**
     * @param schemaId The wire schema id
     * @return The bytes which should prefix the serialized data
     */
    byte[] prefix(W schemaId);

    /**
     * @param schemaId The wire schema id
     * @param site The data being serialized
     * @return The headers which should be added to the record.
     */
    List<Header> headers(W schemaId, RecordDataLocation site);
}
