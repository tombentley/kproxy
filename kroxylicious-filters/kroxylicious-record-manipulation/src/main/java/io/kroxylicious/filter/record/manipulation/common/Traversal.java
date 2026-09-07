/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.List;

/**
 * Focuses on zero or more parts of a larger structure, uniformly.
 * @param <S> the type of the whole structure
 * @param <A> the type of each focused part
 */
public interface Traversal<S, A> {

    /**
     * Reads every focused part.
     * @param s the whole structure
     * @return every focused part, in order
     */
    List<A> getAll(S s);

    /**
     * Transforms every focused part.
     * @param s the whole structure
     * @param f the transformation, given each part and a {@link OpContext}
     * @param opContext the context to pass to {@code f}
     * @return a copy of {@code s} with {@code f} applied to every focused part
     */
    S modifyAll(S s, BaseTypedOp<A, A> f, OpContext opContext);
}
