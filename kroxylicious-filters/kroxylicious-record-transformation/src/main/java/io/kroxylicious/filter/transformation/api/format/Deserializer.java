/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.format;

import java.io.IOException;
import java.io.InputStream;

import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.mapper.TypeCheckable;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaId;

public interface Deserializer<S, V> extends TypeCheckable {

    Type<?, ?, ?> typeCheck(Type<?, ?, ?> type);

    SchemaAndValue<NoSchemaId, S, V> deserialize(InputStream in, Context context) throws IOException;


}
