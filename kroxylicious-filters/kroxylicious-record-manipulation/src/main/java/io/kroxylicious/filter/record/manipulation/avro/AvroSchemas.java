/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import java.util.List;

import org.apache.avro.JsonProperties;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.op.OpConfig;

/**
 * Reads the non-standard {@code apply} keyword off an Avro {@link org.apache.avro.Schema} or
 * {@link org.apache.avro.Schema.Field} - the Avro equivalent of {@code SchemaConfig.apply()}.
 * <p>
 * Unlike JSON, an Avro schema is a real Java object, and both {@link org.apache.avro.Schema} and
 * {@link org.apache.avro.Schema.Field} already extend {@link JsonProperties}, which preserves any
 * JSON property it doesn't itself recognise and exposes it via {@link JsonProperties#getObjectProp(String)}
 * (as plain {@code Map}/{@code List}/primitive Java values, the same shape {@code ObjectMapper} would
 * produce reading into {@code Object.class}). So there is no need for a config model paralleling
 * {@code config.SchemaConfig}: an Avro schema authored with {@code apply} already round-trips through
 * {@code Schema.Parser} keeping it, on both a field (sibling of its {@code type}) and a bare schema
 * (e.g. an array's {@code items}, which is itself a schema and can carry its own extra properties).
 */
public class AvroSchemas {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AvroSchemas() {
    }

    /**
     * Reads the {@code apply} property, if any.
     * @param props the schema or field to read {@code apply} from
     * @return the {@code apply} chain, or {@code null} if {@code props} carries no {@code apply} property
     */
    public static List<OpConfig> applyConfig(JsonProperties props) {
        Object raw = props.getObjectProp("apply");
        if (raw == null) {
            return null;
        }
        return MAPPER.convertValue(raw, new TypeReference<List<OpConfig>>() {
        });
    }
}
