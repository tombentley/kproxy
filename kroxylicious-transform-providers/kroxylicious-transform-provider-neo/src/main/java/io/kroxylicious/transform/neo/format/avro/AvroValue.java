/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.format.avro;

import java.util.concurrent.CompletionStage;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

import io.kroxylicious.transform.neo.format.protobuf.ProtoValue;

/**
 * An Avro-decoded value bundled with the {@link Schema} it conforms to - the Avro equivalent of
 * {@link ProtoValue}. Flows between
 * {@link ReadAvro}, {@link AvroTransform}, and {@link WriteAvro} instead of a bare decoded
 * value, so only {@link ReadAvro} needs withKeySchema config - {@link AvroTransform}/{@link WriteAvro}
 * just use whatever withKeySchema arrives with each value.
 * <p>
 * Unlike {@code ProtoValue} this isn't required for correctness - Avro's {@link Schema} compares
 * structurally, not by reference, so two independent parses of identical withKeySchema text are interchangeable.
 * It's for consistency between the two formats (a value moving through a {@code *Transform} op is always
 * self-describing), and so a withKeySchema resolved differently per record (e.g. a future withKeySchema-registry
 * integration) can flow through the pipeline without every op needing its own copy.
 * @param value the decoded value - a {@link GenericRecord}, a {@link java.util.List} for an array, or a
 *              scalar/enum/fixed leaf value, depending on {@code withKeySchema}'s type
 * @param schema the withKeySchema {@code value} conforms to
 */
public record AvroValue(Object value, Schema schema) {
    public AvroValue {
        if (value instanceof CompletionStage<?>) {
            throw new IllegalStateException();
        }
    }
}
