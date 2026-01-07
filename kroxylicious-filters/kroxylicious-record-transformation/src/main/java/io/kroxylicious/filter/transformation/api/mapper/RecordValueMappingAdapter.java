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
 * Adapts a {@link DataMapping} into a {@link RecordMapping} operating on the record value.
 * This transformation preserves the record key and headers.
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
@SuppressWarnings("java:S6213")
public class RecordValueMappingAdapter<
        K, KS, KI extends WireSchemaId,
        V1, V1S, V1I extends WireSchemaId,
        V2, V2S, V2I extends WireSchemaId>
        implements RecordValueMapping<K, KS, KI,
                V1, V1S, V1I,
                V2, V2S, V2I> {
    DataMapping<V1I, V1S, V1, V2I, V2S, V2> dataMapping;

    @Override
    public MappingRecord<K, KS, KI, V2, V2S, V2I> transform(MappingRecord<K, KS, KI, V1, V1S, V1I> record, Context context) {

        Context context1 = new Context(context.topicName(),
                record.headers(),
                RecordDataLocation.VALUE);

        SchemaAndValue<V2I, V2S, V2> transform = dataMapping.transform(new SchemaAndValue<>(record.valueSchemaId(), record.valueSchema(), record.value()),
                context1);
        ((MappingRecord) record).withValue(transform.value(), transform.schemaId(), transform.schema());
        return ((MappingRecord) record);
    }
}
