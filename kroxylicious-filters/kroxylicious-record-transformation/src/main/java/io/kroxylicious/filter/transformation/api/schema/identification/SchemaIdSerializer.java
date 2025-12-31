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

import io.kroxylicious.filter.transformation.api.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.mapper.TypeCheckable;

/**
 * How schemas are identified during serialization.
 */
public interface SchemaIdSerializer<W extends WireSchemaId> extends TypeCheckable {

    /**
     * @param site The data location.
     * @param schemaId The wire schema id.
     * @param outputStream The stream to write to
     * @return Any additional headers which need to be added to the record.
     */
    List<Header> serializeSchemaId(RecordDataLocation site, W schemaId, OutputStream outputStream) throws IOException;

}
