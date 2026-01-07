/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

/**
 * A specialised {@link RecordMapping} which operates only on the record headers,
 * not modifying the record's key or value.
 * @param <K1> The type of the input record key
 * @param <K1S> The type of the input record key schema
 * @param <K1I> The type of the input record key schema identifier
 * @param <V1> The type of the input record value
 * @param <V1S> The type of the input record value schema
 * @param <V1I> The type of the input record value schema identifier
 * @param <K2> The type of the output record key
 * @param <K2S> The type of the output record key schema
 * @param <K2I> The type of the output record key schema identifier
 * @param <V2> The type of the output record value
 * @param <V2S> The type of the output record value schema
 * @param <V2I> The type of the output record value identifier
 */

public interface RecordHeaderMapping<K1, K1S, K1I extends WireSchemaId,
        V1, V1S, V1I extends WireSchemaId,
        K2, K2S, K2I extends WireSchemaId,
        V2, V2S, V2I extends WireSchemaId>
        extends RecordMapping<
        K1, K1S, K1I,
        V1, V1S, V1I,
        K2, K2S, K2I,
        V2, V2S, V2I> {
}
