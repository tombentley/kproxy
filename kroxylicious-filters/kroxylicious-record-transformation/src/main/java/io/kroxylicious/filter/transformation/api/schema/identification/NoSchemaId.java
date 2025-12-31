/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

/**
 * The absence of a schema identifier on the wire.
 */
public record NoSchemaId() implements WireSchemaId {
    public static final NoSchemaId INSTANCE = new NoSchemaId();

}
