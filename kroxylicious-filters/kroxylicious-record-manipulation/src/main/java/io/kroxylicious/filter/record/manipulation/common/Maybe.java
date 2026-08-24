/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

/**
 * A value that might not be there (the "Option"/"Maybe" type familiar from Haskell, Swift, Rust, Kotlin and
 * F#), used by {@link Property} to report whether it currently focuses on anything.
 * @param <A> the type of the value, when present
 */
public sealed interface Maybe<A> {

    /**
     * Creates a present value.
     * @param value the value
     * @param <A> the type of the value
     * @return a {@link Maybe} wrapping {@code value}
     */
    static <A> Maybe<A> some(A value) {
        return new Some<>(value);
    }

    /**
     * Creates an absent value.
     * @param <A> the type the value would have, were it present
     * @return an absent {@link Maybe}
     */
    static <A> Maybe<A> none() {
        return new None<>();
    }

    /**
     * A present value.
     * @param value the value
     * @param <A> the type of the value
     */
    record Some<A>(A value) implements Maybe<A> {}

    /**
     * An absent value.
     * @param <A> the type the value would have, were it present
     */
    record None<A>() implements Maybe<A> {}
}
