/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * The type-system abstraction shared between operations and the type checker.
 * {@link TypeSystem} maps a foreign type system onto the Java type system via {@link java.lang.reflect.Type},
 * augmented by {@link Union} for representing union types (which Java does not support, but most foreign type
 * systems do). {@link SchemaType} pairs a foreign schema with the {@link TypeSystem} that interprets it into a
 * single navigable "type context"; an operation that reinterprets its input under a new type system advertises
 * the {@code SchemaType} it introduces, and the type checker threads that context while typing paths.
 * <p>
 * These types are format-agnostic (they speak only {@link java.lang.reflect.Type}); concrete bindings and their
 * dependencies (Avro, Jackson, …) live in the format-specific packages that implement {@link TypeSystem}.
 */
@ReturnValuesAreNonnullByDefault
@DefaultAnnotationForParameters(NonNull.class)
@DefaultAnnotation(NonNull.class)
package io.kroxylicious.filter.record.manipulation.type;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault;
