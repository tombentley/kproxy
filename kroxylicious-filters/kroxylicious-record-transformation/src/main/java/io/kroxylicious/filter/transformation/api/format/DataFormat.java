/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.format;

import java.util.Set;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * <p>A data format, like JSON, or Apache Avro.</p>
 *
 * <h3>Encodings</h3>
 * <p>Formats can support multiple encodings.
 * For example Apache Avro supports a binary encoding and a JSON encoding.
 * Where this is the case:</p>
 * <ul>
 *     <li>the encodings must be identified by name, represented elements of an enum,
 *     with legal values returned by {@link #encodings()}.</li>
 *     <li>one of the encodings must be chosen as the <em>default encoding</em>, which is the encoding
 *     used when a null argumment is passed to {@link #deserializer(Enum)} or {@link #serializer(Enum)}.</li>
 * </ul>
 *
 * <h3>Schemas</h3>
 * <p>Some formats, like (schemaless) JSON and XML, are not associated with a schema.
 * Other formats, like Apache Avro or Google Protocol Buffers, must be associated with a particular schema instance,
 * because a schema is needed for (de)serialization.
 * For some other formats, like JSON and XML with schemas, a schema is not strictly required for
 * (de)serialization. But when a schema is not used it cannot be used to validate that the
 * data is schema-valid.</p>
 *
 * @param <S> The type of schema associated with this format.
 * @param <V> The type of the data itself, once deserialized
 */
public interface DataFormat<S, V> {

    /**
     * @return The schema id associated with this format,
     * or {@link io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaId#INSTANCE}.
     */
    WireSchemaId schemaId();

    /**
     * @return The default encoding to be used if a specific encoding is not defined.
     */
    String defaultEncoding();

    /**
     * @return The set of encodings supported by this format
     */
    Set<String> encodings();

    /**
     * @param encoding The encoding to be used by the serializer, or null for the default encoding for this format.
     * @return A serializer for the given format.
     */
    Serializer<V> serializer(String encoding);

    /**
     * @param encoding The encoding to be used by the deserializer, or null for the default encoding for this format.
     * @return A deserializer for the given format.
     */
    Deserializer<S, V> deserializer(String encoding);
    // Validator<V> validator() TODO
}
