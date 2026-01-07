/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

/**
 * A specialised {@link RecordMapping} which operates only on the record's value,
 * not modifying the record's key or headers.
 * @param <K> The type of the input and output record key
 * @param <KS> The type of the input and output record key schema
 * @param <KI> The type of the input and output record key schema identifier
 * @param <V1> The type of the input record value
 * @param <V1S> The type of the input record value schema
 * @param <V1I> The type of the input record value schema identifier
 * @param <V2> The type of the output record value
 * @param <V2S> The type of the output record value schema
 * @param <V2I> The type of the output record value schema identifier
 */
public interface RecordValueMapping<
        K, KS, KI extends WireSchemaId,
        V1, V1S, V1I extends WireSchemaId,
        V2, V2S, V2I extends WireSchemaId>
        extends RecordMapping<K, KS, KI,
        V1, V1S, V1I,
        K, KS, KI,
        V2, V2S, V2I> {}
