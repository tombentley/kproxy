/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.format;

import java.io.IOException;
import java.io.InputStream;

import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.Mapper;

public interface Deserializer<T> extends Mapper<InputStream, T> {

    default Class<InputStream> acceptedType() {
        return InputStream.class;
    }

    Class<T> returnedType();

    T deserialize(InputStream in, Context context) throws IOException;

    default T transform(InputStream in, Context context) {
        try {
            return deserialize(in, context);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
