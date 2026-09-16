/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Provides JSON Path-like functionality over trees of arbitrary types.
 * {@link Path} is somewhat like the syntax tree of JSON Path.
 * However, the evaluation and type systems are not the same as JSON path.
 * Evaluation via {@link PathEvaluator#eval(Object, List)} supports parallelized evaluation of multiple paths.
 * {@link TypeSystem} provides a mapping from a foreign type system onto the Java type system via {@link java.lang.reflect.Type},
 * augmented by {@link Union} for representing union types (which Java does not support, but most foreign type systems do).
 * {@link TypeEvaluator} allows to combine a foreign schema and its type system with a path and determine
 * the possible types of the result when that path is evaluated against an instance value conforming to the schema.
 */
@ReturnValuesAreNonnullByDefault
@DefaultAnnotationForParameters(NonNull.class)
@DefaultAnnotation(NonNull.class)
package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.List;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault;