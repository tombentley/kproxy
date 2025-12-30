/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public record SchemaAndValue<S, V>(WireSchemaId schemaId, S schema, V value) {

}
