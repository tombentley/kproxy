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

import io.kroxylicious.filter.transformation.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.mapper.TypeCheckable;

/**
 * How schemas are identified during serialization.
 */
public interface OutputSchemaIdentification<W extends WireSchemaId> extends TypeCheckable {

    /**
     * @param schemaId The wire schema id
     * @return Headers which might need to be added to the record
     */
    List<Header> prefix(RecordDataLocation site, W schemaId, OutputStream outputStream) throws IOException;

}
