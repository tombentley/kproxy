/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.InputStream;

public record SchemaTaggedStream<W extends WireSchemaId>(W schemaId, InputStream rest) {
}
