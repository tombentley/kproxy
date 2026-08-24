/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.protobuf;

import java.util.List;
import java.util.Map;

import com.google.protobuf.Descriptors;

import io.kroxylicious.filter.record.manipulation.config.ApplyConfig;

/**
 * The result of {@link ProtoSchemaParser#parse(String, String)}: the root message's real
 * {@link Descriptors.Descriptor}, plus a side-table of {@code apply} chains read off the schema's
 * custom field/message options.
 * <p>
 * A real {@link Descriptors.FieldDescriptor}/{@link Descriptors.Descriptor} has nowhere to carry a
 * non-standard {@code apply} keyword the way Avro's {@code Schema}/{@code Schema.Field} do (they extend
 * {@link org.apache.avro.JsonProperties}, which round-trips unknown JSON properties for free) - the
 * conversion this module reuses (Apicurio's {@code FileDescriptorUtils}) only ever translates a fixed set
 * of well-known option names into the built descriptor's options, silently dropping anything else. So
 * {@code apply} chains are read directly off the parsed schema's option syntax during parsing and kept
 * alongside the descriptor here, keyed by the {@link Descriptors.GenericDescriptor} (a common supertype
 * of {@link Descriptors.Descriptor} and {@link Descriptors.FieldDescriptor}) they were declared on.
 * @param descriptor the root message type's descriptor
 * @param apply {@code apply} chains declared in the schema, keyed by the field or message they attach to
 */
public record ParsedProtoSchema(Descriptors.Descriptor descriptor, Map<Descriptors.GenericDescriptor, List<ApplyConfig>> apply) {}
