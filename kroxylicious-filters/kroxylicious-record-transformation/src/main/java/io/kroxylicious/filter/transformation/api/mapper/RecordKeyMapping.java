/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

/**
 * A specialised {@link RecordMapping} which operates only on the record's key,
 * not modifying the record's value or headers
 * @param <K1> The type of the input record key
 * @param <K1S> The type of the input record key schema
 * @param <K1I> The type of the input record key schema identifier
 * @param <V> The type of the input and output record value
 * @param <VS> The type of the input and output record value schema
 * @param <VI> The type of the input and output record value schema identifier
 * @param <K2> The type of the output record key
 * @param <K2S> The type of the output record key schema
 * @param <K2I> The type of the output record key schema identifier
 */
public interface RecordKeyMapping<
        K1, K1S, K1I extends WireSchemaId,
        V, VS, VI extends WireSchemaId,
        K2, K2S, K2I extends WireSchemaId>
        extends RecordMapping<K1, K1S, K1I,
        V, VS, VI,
        K2, K2S, K2I,
        V, VS, VI> {

}
