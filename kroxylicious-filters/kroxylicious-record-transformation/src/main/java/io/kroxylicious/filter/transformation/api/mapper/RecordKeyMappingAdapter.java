/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import io.kroxylicious.filter.transformation.api.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

/**
 * Adapts a {@link DataMapping} into a {@link RecordMapping} operating on the record key.
 * This transformation preserves the record value and headers.
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
@SuppressWarnings("java:S6213")
public class RecordKeyMappingAdapter<
        K1, K1S, K1I extends WireSchemaId,
        V, VS, VI extends WireSchemaId,
        K2, K2S, K2I extends WireSchemaId>
        implements RecordKeyMapping<K1, K1S, K1I,
                V, VS, VI,
                K2, K2S, K2I> {

    DataMapping<K1I, K1S, K1, K2I, K2S, K2> dataMapping;

    @Override
    public MappingRecord<K2, K2S, K2I, V, VS, VI> transform(MappingRecord<K1, K1S, K1I, V, VS, VI> record, Context context) {

        Context context1 = new Context(context.topicName(),
                record.headers(),
                RecordDataLocation.KEY);
        SchemaAndValue<K2I, K2S, K2> transform = dataMapping.transform(new SchemaAndValue<>(record.keySchemaId(), record.keySchema(), record.key()),
                context1);
        ((MappingRecord) record).withKey(transform.value(), transform.schemaId(), transform.schema());
        return ((MappingRecord) record);
    }
}
