/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

/**
 * The schema identification strategy used by Confluent Schema Registry: a 5 byte prefix to the data.
 * The first byte is the zero ('magic') byte, followed by a 4 byte identifier.
 */
public class ConfluentSchemaIdentificationStrategy extends PrefixedDataIdentificationStrategy {

    public static final ConfluentSchemaIdentificationStrategy INSTANCE = new ConfluentSchemaIdentificationStrategy();

    ConfluentSchemaIdentificationStrategy() {
        super(0x00, 5);
    }
}

