/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.IOException;
import java.io.InputStream;

import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.TypeCheckable;

/**
 * A Deserializer that identifies a schema for the data that it is to read.
 * If the implementation of {@link #deserialize(InputStream, Context)} needs to read
 * from the input stream to do this then the stream should be left so
 * that the next call to read will return the start of the data.
 * @param <W> The type of the schema id
 */
public interface SchemaIdDeserializer<W extends WireSchemaId> extends TypeCheckable {

    /**
     * Obtains the schema id, reading from the given {@code stream} it necessary.
     *
     * @param stream The stream to be read from
     * @param context The context
     * @return The schema id.
     */
    SchemaAndValue<W, Void, InputStream> deserialize(InputStream stream, Context context) throws IOException;
}
