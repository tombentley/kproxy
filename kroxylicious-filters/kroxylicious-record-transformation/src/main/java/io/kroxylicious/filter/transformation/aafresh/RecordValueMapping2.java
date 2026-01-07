/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import io.kroxylicious.filter.transformation.api.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.RecordMapping;

/**
 * A specialised {@link RecordMapping} which operates only on the record's key,
 * not modifying the record's value or headers
 */
public class RecordValueMapping2<
        K,
        V1,
        V2>
        implements RecordMapping2<K,
        V1,
        K,
        V2> {
    DataMapping2<V1, V2> dataMapping;

    @Override
    public MappingRecord2<K, V2> transform(MappingRecord2<K, V1> record, Context context) {
        Context context1 = new Context(context.topicName(),
                record.headers(),
                RecordDataLocation.VALUE);
        var transformd = dataMapping.transform(record.value(), context1);
        return new MappingRecord2<>(record.headers(), record.key(), transformd);
    }
}
