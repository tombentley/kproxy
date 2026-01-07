/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh.avro;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.avro.SchemaParser;

import io.kroxylicious.filter.transformation.aafresh.SchemaFactory;

public class AvroSchemaFactory implements SchemaFactory {
    @Override
    public Object parseSchema(byte[] schemaBytes) {
        try {
            SchemaParser parser = new SchemaParser();
            SchemaParser.ParseResult parse = parser.parse(new ByteArrayInputStream(schemaBytes));
            return parse.mainSchema();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
