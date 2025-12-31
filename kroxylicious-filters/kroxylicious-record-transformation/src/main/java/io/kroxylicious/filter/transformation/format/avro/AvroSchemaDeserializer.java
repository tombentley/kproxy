/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.avro;

import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.SchemaParser;

/**
 * Encapsulate Avro schema parsing.
 * Compose with a {@link AvroDeserializerFactory} to obtain a deserializer for Avro data.
 */
public class AvroSchemaDeserializer {

    public Schema deserialize(InputStream in) throws IOException {
        SchemaParser parser = new SchemaParser();
        SchemaParser.ParseResult parse = parser.parse(in);
        return parse.mainSchema();
    }
}
