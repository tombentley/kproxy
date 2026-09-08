/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

/**
 * An Avro-decoded value bundled with the {@link Schema} it conforms to - the Avro equivalent of
 * {@link io.kroxylicious.filter.record.manipulation.format.protobuf.ProtoValue}. Flows between
 * {@link DeserializeAvro}, {@link AvroTransform}, and {@link SerializeAvro} instead of a bare decoded
 * value, so only {@link DeserializeAvro} needs schema config - {@link AvroTransform}/{@link SerializeAvro}
 * just use whatever schema arrives with each value.
 * <p>
 * Unlike {@code ProtoValue} this isn't required for correctness - Avro's {@link Schema} compares
 * structurally, not by reference, so two independent parses of identical schema text are interchangeable.
 * It's for consistency between the two formats (a value moving through a {@code *Transform} op is always
 * self-describing), and so a schema resolved differently per record (e.g. a future schema-registry
 * integration) can flow through the pipeline without every op needing its own copy.
 * @param value the decoded value - a {@link GenericRecord}, a {@link java.util.List} for an array, or a
 *              scalar/enum/fixed leaf value, depending on {@code schema}'s type
 * @param schema the schema {@code value} conforms to
 */
public record AvroValue(Object value, Schema schema) {}
