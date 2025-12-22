/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

public record Datum<T>(SchemaIdentifier schemaIdentifier, Class<T> type, T datum) {
}