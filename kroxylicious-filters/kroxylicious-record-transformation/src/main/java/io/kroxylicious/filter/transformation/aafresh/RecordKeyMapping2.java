/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import io.kroxylicious.filter.transformation.api.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.DataMapping;
import io.kroxylicious.filter.transformation.api.mapper.MappingRecord;
import io.kroxylicious.filter.transformation.api.mapper.RecordMapping;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

/**
 * A specialised {@link RecordMapping} which operates only on the record's key,
 * not modifying the record's value or headers
 * @param <K1> The type of the input record key
 * @param <V> The type of the input and output record value
 * @param <K2> The type of the output record key
 */
public class RecordKeyMapping2<
        K1,
        V,
        K2>
        implements RecordMapping2<K1,
        V,
        K2,
        V> {
    DataMapping2<K1, K2> dataMapping;

    @Override
    public MappingRecord2<K2, V> transform(MappingRecord2<K1, V> record, Context context) {

        Context context1 = new Context(context.topicName(),
                record.headers(),
                RecordDataLocation.KEY);
        var transformd = dataMapping.transform(record.key(), context1);
        return new MappingRecord2<>(record.headers(), transformd, record.value());
    }
}
