/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.type;

/**
 * The unit type of the <em>present null value</em>: the type inhabited by exactly one value,
 * {@link #INSTANCE}. A JSON {@code null} (Jackson {@code NullNode}), a Kafka null key/value, and an Avro
 * null are all per-domain encodings of this one value.
 * <p>
 * {@code Null} is a value that <em>is there</em>, unlike {@link Nothing} (absence): {@code x == null} is
 * {@code true} for {@code Null} but {@code false} for {@link Nothing}, and navigating into a {@code Null}
 * (e.g. {@code x.y}) yields {@link Nothing}. Keeping the two apart is what lets the language honour
 * RFC 9535's distinction between a query that selects a JSON {@code null} and one that selects no node,
 * and what lets a Kafka tombstone (null value) be detected with {@code $.value == null}.
 * <p>
 * Java {@code null} is <em>not</em> a value of this type in the evaluator's internal algebra: it exists
 * only as the Kafka-domain encoding of {@code Null}, normalised to {@link #INSTANCE} at the evaluation
 * boundary and re-materialised to a Java null on egress.
 */
public final class Null {

    /** The sole value of the {@code Null} type: the canonical present-null marker. */
    public static final Null INSTANCE = new Null();

    private Null() {
    }

    @Override
    public String toString() {
        return "Null";
    }
}
