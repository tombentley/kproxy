/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.type;

/**
 * The unit type of <em>absence</em> ("Nothing"): the type inhabited by exactly one value,
 * {@link #INSTANCE}, representing "no value at all" — for example a singular query that selects no node,
 * an out-of-range index, or a function declared to sometimes return nothing.
 * <p>
 * {@code Nothing} is deliberately distinguished from two neighbours it is easy to conflate:
 * <ul>
 *   <li>{@link Null} — a <em>present</em> null value (e.g. a JSON {@code null}). {@code x == null} is
 *       {@code true} for {@link Null} but {@code false} for {@code Nothing}; navigating into either
 *       yields {@code Nothing}.</li>
 *   <li>{@link Bottom} — the <em>uninhabited</em> type (no values at all). {@code Nothing} has exactly
 *       one value; {@link Bottom} has none.</li>
 * </ul>
 * Optionality is modelled as a union with this type, i.e. {@code Union.of(T, Nothing.class)}; the top
 * type is structurally {@code Nothing} summed with every other type. The per-domain runtime encodings of
 * the single {@code Nothing} value (Jackson's {@code MissingNode}, {@link io.kroxylicious.filter.record.manipulation.ops.common.Maybe.None},
 * a Kafka absent field) are bridged to {@link #INSTANCE} at the evaluation boundary.
 */
public final class Nothing {

    /** The sole value of the {@code Nothing} type: the canonical "absent" marker. */
    public static final Nothing INSTANCE = new Nothing();

    private Nothing() {
    }

    @Override
    public String toString() {
        return "Nothing";
    }
}
