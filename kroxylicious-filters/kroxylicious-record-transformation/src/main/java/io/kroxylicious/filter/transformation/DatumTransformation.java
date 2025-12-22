/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.util.List;

import org.apache.kafka.common.header.Header;

record DatumTransformation(
        DatumDeserializer<?> deserializer,
        List<DatumMapper<?, ?>> mappers,
        DatumSerializer<?> serializer
) {
    DatumTransformation {
        var type = deserializer.returnedType();
        for (DatumMapper<?, ?> mapper : mappers) {
            if (!mapper.acceptedType().isAssignableFrom(type)) {
                throw new IllegalArgumentException("The mapper cannot accept values of type " + type.getName());
            }
            type = mapper.returnedType();
        }
        if (!serializer.acceptedType().isAssignableFrom(type)) {
            throw new IllegalArgumentException("The serializer cannot accept values of type " + type.getName());
        }
    }

    void apply(Header[] headers, TransformationInputStream in, TransformationOutputStream out) throws IOException {
        var datum = deserializer.deserialize(headers, in);
        for (DatumMapper mapper : mappers) {
            datum = mapper.transform(datum);
        }
        ((DatumSerializer) serializer).serialize(datum, out);
    }
}
