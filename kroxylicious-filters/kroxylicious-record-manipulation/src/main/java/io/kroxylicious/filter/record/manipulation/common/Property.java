/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.BiFunction;

/**
 * Focuses on a part of a larger structure that might not be there (the "Optional"/"AffineTraversal" optic in
 * the Lens/Prism/Traversal family - a {@code Lens} that might not focus on anything).
 * @param <S> the type of the whole structure
 * @param <A> the type of the focused part
 */
public interface Property<S, A> {

    /**
     * Reads the focused part.
     * @param s the whole structure
     * @return the focused part, or {@link Maybe#none()} if it isn't there
     */
    Maybe<A> get(S s);

    /**
     * Writes the focused part.
     * @param s the whole structure
     * @param a the new value
     * @return a copy of {@code s} with the focused part set to {@code a}
     */
    S set(S s, A a);

    /**
     * Removes the focused part.
     * @param s the whole structure
     * @return a copy of {@code s} with the focused part absent
     */
    S clear(S s);

    /**
     * Reads, transforms and writes back the focused part in one step.
     * @param s the whole structure
     * @param f the transformation, given the current (possibly absent) value and a {@link Context}
     * @param context the context to pass to {@code f}
     * @return the result of writing {@code f}'s result back via {@link #set(Object, Object)} (if present) or
     *         {@link #clear(Object)} (if absent)
     */
    default S modify(S s, BiFunction<Maybe<A>, Context, Maybe<A>> f, Context context) {
        return switch (f.apply(get(s), context)) {
            case Maybe.Some<A> some -> set(s, some.value());
            case Maybe.None<A> none -> clear(s);
        };
    }
}
