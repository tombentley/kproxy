/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.transformation.aafresh.Deser;
import io.kroxylicious.filter.transformation.aafresh.DeserializerFactory;
import io.kroxylicious.filter.transformation.aafresh.Format;

import edu.umd.cs.findbugs.annotations.Nullable;

public class JacksonDeserFactory implements DeserializerFactory {
    @Override
    public @Nullable Deser<?> deser(Format<?> format) {
        if ("json".equals(format.formatName())
                && "json".equals(format.encoding())) {
            return new JsonDeser(new ObjectMapper());
        }
        return null;
    }
}
