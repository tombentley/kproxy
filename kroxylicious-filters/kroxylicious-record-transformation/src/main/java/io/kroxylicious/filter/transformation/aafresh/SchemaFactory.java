/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import java.io.IOException;

/**
 * Abstracts the conversion of a serialized schema into a schema object.
 * @param <S>
 */
public interface SchemaFactory<S> {

    S parseSchema(byte[] schemaBytes);
}
