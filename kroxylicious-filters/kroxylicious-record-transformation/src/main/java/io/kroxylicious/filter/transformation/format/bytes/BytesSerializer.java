/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.format.bytes;

import java.io.IOException;
import java.io.OutputStream;

import io.kroxylicious.filter.transformation.TransformationInputStream;
import io.kroxylicious.filter.transformation.TransformationOutputStream;
import io.kroxylicious.filter.transformation.api.TypeException;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.format.Serializer;

public class BytesSerializer implements Serializer<TransformationInputStream> {

    public static final BytesSerializer INSTANCE = new BytesSerializer();

    @Override
    public void accepts(Type<?, ?, ?> type) {
        if (type.cls() != TransformationInputStream.class) {
            throw new TypeException("");
        }
    }

    @Override
    public void serialize(TransformationInputStream value, OutputStream out) throws IOException {
        if (out instanceof TransformationOutputStream) {
            value.offer((TransformationOutputStream) out);
        }
        else {
            value.transferTo(out);
        }
    }
}
