/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh.avro;

import org.apache.avro.Schema;

import io.kroxylicious.filter.transformation.aafresh.Deser;
import io.kroxylicious.filter.transformation.aafresh.DeserializerFactory;
import io.kroxylicious.filter.transformation.aafresh.Format;

import edu.umd.cs.findbugs.annotations.Nullable;

public class AvroDeserFactory implements DeserializerFactory {
    @Nullable
    @Override
    public Deser<?> deser(Format<?> format) {
        if (format.formatName().equals("avro")
                && format.encoding().equals("binary")) {
            return new AvroDerser((Schema) format.schema());
        }
        return null;
    }
}
