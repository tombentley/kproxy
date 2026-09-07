/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import io.kroxylicious.filter.record.manipulation.common.Requirement;
import io.kroxylicious.filter.record.manipulation.format.DataFormat;
import io.kroxylicious.filter.record.manipulation.format.DataFormatService;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;

public class JacksonJsonFormatService implements DataFormatService<JsonNode, Void, SchemaConfig> {
    @Override
    public DataFormat<JsonNode> create(Void schemaConfiguration) {
        return new JacksonJsonFormat();
    }

    @Override
    public BaseTypedOp<JsonNode, JsonNode> operator(SchemaConfig opSchema) {
        return JacksonFunction.buildMask(opSchema, Set.of(Requirement.TYPE_PRESERVING), null);
    }
}
