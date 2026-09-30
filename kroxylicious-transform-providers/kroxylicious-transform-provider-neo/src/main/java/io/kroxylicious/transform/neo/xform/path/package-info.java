/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Provides JSON Path-like functionality over trees of arbitrary types.
 * {@link Ast.Path} is somewhat like the syntax tree of JSON Path.
 * However, the evaluation and type systems are not the same as JSON path.
 * {@link AstEvaluator} compiles a resolved, type-checked {@link Ast} into reusable evaluators — queries
 * ({@link SeqEval}), filter predicates and values — over a {@link TreeAdapter}.
 * {@link TypeSystem} provides a mapping from a foreign type system onto the Java type system via {@link java.lang.reflect.Type},
 * augmented by {@link Union} for representing union types (which Java does not support, but most foreign type systems do).
 * {@link SchemaType} pairs a foreign withKeySchema with its {@link TypeSystem} into a single navigable type context.
 * {@link TypeCheckingVisitor} threads such a context while type checking, using it to determine the possible types of the
 * result when a path is evaluated against an instance value conforming to the withKeySchema.
 */
@ReturnValuesAreNonnullByDefault
@DefaultAnnotationForParameters(NonNull.class)
@DefaultAnnotation(NonNull.class)
package io.kroxylicious.transform.neo.xform.path;

import io.kroxylicious.transform.neo.type.SchemaType;
import io.kroxylicious.transform.neo.type.TypeSystem;
import io.kroxylicious.transform.neo.type.Union;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault;