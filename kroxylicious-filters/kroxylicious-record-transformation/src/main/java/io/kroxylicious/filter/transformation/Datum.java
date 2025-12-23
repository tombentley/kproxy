/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

/**
 * <p>A typed value in a {@linkplain DatumTransformation transformation pipeline}.
 * There are two kinds of type information:</p>
 * <ul>
 * <li>The schemaIdentifier is the nominal external type.</li>
 * <li>The type is the Java type. This is used to provide basic type checking on the assembled pipeline,
 * for example that the user is not trying to serialize as a String data which was read as an Integer.</li>
 * </ul>
 * @param schemaIdentifier
 * @param type
 * @param datum
 * @param <T>
 */
public record Datum<T>(SchemaIdentifier schemaIdentifier, Class<T> type, T datum) {
}