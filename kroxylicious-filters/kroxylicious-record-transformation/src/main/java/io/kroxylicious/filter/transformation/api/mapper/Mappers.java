/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import java.util.List;

import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaId;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public class Mappers {
    private Mappers() {
    }

    /**
     * A mapping that replaces removes schema information
     */
    public static <W extends WireSchemaId, S, V> DataMapping<W, S, V, NoSchemaId, S, V> noSchemaId() {
        return withSchemaId(NoSchemaId.INSTANCE);
    }

    /**
     * A mapping that replaces the wireSchemaId with the given value
     */
    static <W2 extends WireSchemaId, W extends WireSchemaId, S, V> DataMapping<W, S, V, W2, S, V> withSchemaId(W2 schemaId) {
        return new SchemaIdReplacement<>(schemaId);
    }

    /**
     * @return The identity mapping
     */
    static <W extends WireSchemaId, S, V> DataMapping<W, S, V, W, S, V> identity() {
        return new IdentityDataMapping<>();
    }

    static boolean isIdentity(DataMapping<?, ?, ?, ?, ?, ?> mapping) {
        return mapping instanceof IdentityDataMapping;
    }

    /**
     * A mapping that composes two other mappings
     * @param first the first mapping to be applied
     * @param andThen the second mapping, which will be applied to the result of the first mapping
     * @return The composed mapping
     */
    static <W1 extends WireSchemaId, S1, V1, W2 extends WireSchemaId, S2, V2, W3 extends WireSchemaId, S3, V3> DataMapping<W1, S1, V1, W3, S3, V3> compose(
            DataMapping<W1, S1, V1, W2, S2, V2> first,
            DataMapping<W2, S2, V2, W3, S3, V3> andThen) {
        return new DataMapping<>() {

            @Override
            public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
                var intermediateType = first.typeCheck(type);
                return andThen.typeCheck(intermediateType);
            }

            @Override
            public SchemaAndValue<W3, S3, V3> transform(SchemaAndValue<W1, S1, V1> value, Context context) {
                var intermediateResult = first.transform(value, context);
                return andThen.transform(intermediateResult, context);
            }
        };
    }

    public static DataMapping<?, ?, ?, ?, ?, ?> compose(List<DataMapping<?, ?, ?, ?, ?, ?>> mappers) {
        if (mappers.isEmpty()) {
            return identity();
        }
        if (mappers.size() == 1) {
            return mappers.get(0);
        }
        DataMapping<?, ?, ?, ?, ?, ?> result = null;
        for (DataMapping<?, ?, ?, ?, ?, ?> mapper : mappers) {
            if (result == null) {
                result = mapper;
            }
            result = compose((DataMapping) result, (DataMapping) mapper);
        }
        return result;
    }

    public static HeaderMapping emptyHeaders() {
        return (headers, context) -> List.of();
    }

    public static HeaderMapping identityHeaders() {
        return (headers, context) -> headers;
    }

    private static class IdentityDataMapping<W extends WireSchemaId, S, V> implements DataMapping<W, S, V, W, S, V> {
        @Override
        public SchemaAndValue<W, S, V> transform(SchemaAndValue<W, S, V> value, Context context) {
            return value;
        }

        @Override
        public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
            return type;
        }
    }

    private static class SchemaIdReplacement<W2 extends WireSchemaId, W extends WireSchemaId, S, V> implements DataMapping<W, S, V, W2, S, V> {

        private final W2 schemaId;

        SchemaIdReplacement(W2 schemaId) {
            this.schemaId = schemaId;
        }

        @Override
        public SchemaAndValue<W2, S, V> transform(SchemaAndValue<W, S, V> value, Context context) {
            return value.withSchemaId(schemaId);
        }

        @Override
        public Type<W, ?, ?> typeCheck(Type<?, ?, ?> type) {
            Class<W> aClass = (Class) schemaId.getClass();
            return new Type<>(aClass, type.schema(), type.cls());
        }
    }
}
