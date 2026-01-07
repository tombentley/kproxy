/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.RecordHeaderMapping;
import io.kroxylicious.filter.transformation.api.mapper.RecordKeyMapping;
import io.kroxylicious.filter.transformation.api.mapper.RecordValueMapping;

/**
 * <p>A general mapping/transformation of Kafka Records.
 * Such a transformation can make arbitrary changes to a record's key, value and/or headers.</p>
 *
 * <p>See also the specialised subinterfaces {@link RecordHeaderMapping},
 * {@link RecordKeyMapping} and
 * {@link RecordValueMapping}.</p>
 *
 * @param <K1> The type of the input record key
 * @param <V1> The type of the input record value
 * @param <K2> The type of the output record key
 * @param <V2> The type of the output record value
 *
 */
@SuppressWarnings("java:S6213")
public interface RecordMapping2<
        K1,
        V1,
        K2,
        V2> {
    MappingRecord2<K2, V2> transform(MappingRecord2<K1, V1> record, Context context);
}
