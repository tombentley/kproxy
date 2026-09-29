/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.type;

import java.lang.reflect.Type;
import java.util.List;

public interface TypeSystem<S> {
    /**
     * The top type (&top;): the most general type of this type system, a supertype of every value type it
     * can produce. In a type system without declarative subclassing this is the union of all its value
     * types (structurally, {@link Nothing} summed with every other type). Used when navigation cannot be
     * narrowed, e.g. a descendant ({@code ..}) segment.
     */
    Type topType();

    ///** Create a union type from the given types */
    // Type unionType(List<Type> caseTypes);

    ///** Get the cases which make up the given union type */
    // Set<Type> caseTypes(S unionSchema);

    /** Determine whether the given type is a union type */
    boolean isUnionType(S schema);

    /** Determine whether the given type is an "object" type */
    boolean isObjectType(S schema);

    /** Determine whether the given object type allows additional properties beyond those enumerated by {@link #objectProperties(S)}. */
    boolean isObjectOpen(S objectSchema);

    /** Enumerates the known properties of the given object type. */
    List<String> objectProperties(S objectSchema);

    /**
     * The type of the given {@code property} of the given object type.
     * If {@code property} is not one of those returned by {@link #objectProperties(Object)} then
     * the result should be the same as would be returned by {@link #objectPropertySchema(Object)}.
     */
    S objectPropertySchema(S objectSchema, String propertyName);

    /**
     * The type of some unknown property of the given object type.
     * "Unknown" means, not in the list of properties returned by {@link #objectProperties(Object)}.
     */
    S objectPropertySchema(S objectSchema);

    /**
     * Determine whether the given type is an array type.
     */
    boolean isArrayType(S schema);

    /**
     * The type of the item at the given 0-based index of the given array type.
     */
    boolean isArrayOpen(S arraySchema);

    int[] arrayIndexes(S arraySchema);

    S arrayItemSchema(S arraySchema, int index);

    S arrayItemSchema(S arraySchema);

    /**
     * The bottom type (&perp;): the uninhabited type, with no values. It is the type of something which
     * cannot exist, such as an indexed access of an object-typed value. Implementations should return
     * {@link Bottom Bottom.class}. This is distinct from {@link Nothing} (a unit type with exactly one
     * value, "absent") — the two must not be conflated.
     */
    Type bottomType();

    /**
     * The Java type corresponding to the given schema type
     * @param schema The schema
     * @return The Java type.
     */
    Type typeOf(S schema);
}
