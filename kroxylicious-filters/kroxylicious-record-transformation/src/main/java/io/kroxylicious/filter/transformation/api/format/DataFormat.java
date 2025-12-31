/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.format;

import java.util.Set;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

import edu.umd.cs.findbugs.annotations.Nullable;

public interface DataFormat<E extends Enum<E>, S, V> {

    WireSchemaId schemaId();

    /**
     * @return The set of encodings supported by this format
     */
    Set<E> encodings();

    /**
     * @param encoding The encoding to be used by the serializer, or null for the default encoding for this format.
     * @return A serializer for the given format.
     */
    Serializer<V> serializer(@Nullable E encoding);

    /**
     * @param encoding The encoding to be used by the deserializer, or null for the default encoding for this format.
     * @return A deserializer for the given format.
     */
    Deserializer<S, V> deserializer(@Nullable E encoding);
    // Validator<V> validator() TODO
}
