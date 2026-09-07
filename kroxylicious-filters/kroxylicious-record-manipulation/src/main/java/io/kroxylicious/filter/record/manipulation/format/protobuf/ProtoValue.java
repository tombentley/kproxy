/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.protobuf;

import com.google.protobuf.DynamicMessage;

/**
 * A {@link DynamicMessage} bundled with the {@link ParsedProtoSchema} it was decoded against - the value
 * that flows between {@link DeserializeProtobuf}, {@link ProtobufTransform}, and {@link SerializeProtobuf}.
 * <p>
 * Unlike Avro's {@code Schema}/{@code GenericRecord} (structural equality, field access by name),
 * Protobuf's {@link com.google.protobuf.Descriptors.Descriptor}/{@link com.google.protobuf.Descriptors.FieldDescriptor}
 * use reference identity - {@link DynamicMessage#getField(com.google.protobuf.Descriptors.FieldDescriptor)}
 * throws unless the field descriptor came from the exact same parsed {@code Descriptor} instance as the
 * message. Carrying the schema alongside the message, rather than having every op independently re-parse
 * its own copy of the same {@code .proto} text, guarantees that identity lines up by construction - only
 * {@link DeserializeProtobuf} ever parses; downstream ops just read whatever schema arrived with the
 * value. It also means a future per-record or schema-registry-resolved descriptor could flow through
 * unchanged, without any downstream op needing to change.
 * @param message the decoded message
 * @param schema the schema {@code message} conforms to, plus its {@code apply} chains
 */
public record ProtoValue(DynamicMessage message, ParsedProtoSchema schema) {}
