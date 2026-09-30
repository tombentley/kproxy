/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.neo.type;

import java.lang.reflect.Type;

import io.leangen.geantyref.TypeFactory;

/**
 * The type constructor for a <em>node list</em> ({@code Seq<X>}): the result of a (general, non-singular)
 * query — a sequence of zero or more values of element type {@code X}.
 * <p>
 * In the type lattice {@code Seq<X>} is the general form of a query result (RFC 9535's "NodesType",
 * generalised over an arbitrary element type). The RFC's implicit conversions become explicit coercions
 * applied at defined syntactic sites:
 * <ul>
 *   <li>existence, in a filter {@code testExpr}: {@code Seq<X> -> boolean} (is the list non-empty?);</li>
 *   <li>a singular query used as a comparison operand or scalar function argument: {@code Seq<X> -> X ⊕ Nothing}
 *       (the sole element, or {@link Nothing} if the list is empty);</li>
 *   <li>a top-level query: the list is materialised to the field's required type.</li>
 * </ul>
 * A function parameter typed {@code Seq<X>} receives the whole node list, so node-list-consuming functions
 * type-check by ordinary assignability with no special-casing.
 *
 * @param <X> the element type
 */
public interface Seq<X> extends Iterable<X> {

    /**
     * The parameterized {@link Type} {@code Seq<elementType>}, for use in the type lattice.
     * @param elementType the element type {@code X}
     * @return the {@link Type} reflecting {@code Seq<X>}
     */
    static Type of(Type elementType) {
        return TypeFactory.parameterizedClass(Seq.class, elementType);
    }
}
