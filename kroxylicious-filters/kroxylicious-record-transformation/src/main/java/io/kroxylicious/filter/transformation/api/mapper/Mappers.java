/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import java.util.List;

import io.kroxylicious.filter.transformation.api.SchemaAndValue;
import io.kroxylicious.filter.transformation.api.Type;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchema;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public class Mappers {
    private Mappers() {
    }

    static <W extends WireSchemaId, S, T> DataMapping<S, T, S, T> withSchemaId(W schemaId) {
        return new DataMapping<>() {
            @Override
            public SchemaAndValue<S, T> transform(SchemaAndValue<S, T> value, Context context) {
                return new SchemaAndValue<>(schemaId, value.schema(), value.value());
            }

            @Override
            public Type<W, ?, ?> typeCheck(Type<?, ?, ?> type) {
                Class<W> aClass = (Class) schemaId.getClass();
                return new Type<>(aClass, type.schema(), type.cls());
            }
        };
    }

    static <S, T> DataMapping<S, T, S, T> identity() {
        return new DataMapping<>() {
            @Override
            public SchemaAndValue<S, T> transform(SchemaAndValue<S, T> value, Context context) {
                return value;
            }

            @Override
            public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
                return type;
            }
        };
    }

    static <S, T, U, V, W, X> DataMapping<S, T, W, X> compose(DataMapping<S, T, U, V> first, DataMapping<U, V, W, X> andThen) {
        return new DataMapping<>() {

            @Override
            public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
                var intermediateType = first.typeCheck(type);
                return andThen.typeCheck(intermediateType);
            }

            @Override
            public SchemaAndValue<W, X> transform(SchemaAndValue<S, T> value, Context context) {
                var intermediateResult = first.transform(value, context);
                return andThen.transform(intermediateResult, context);
            }
        };
    }

    public static DataMapping<?, ?, ?, ?> compose(List<DataMapping<?, ?, ?, ?>> mappers) {
        if (mappers.isEmpty()) {
            return identity();
        }
        if (mappers.size() == 1) {
            return mappers.get(0);
        }
        DataMapping<?, ?, ?, ?> result = null;
        for (DataMapping<?, ?, ?, ?> mapper : mappers) {
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

    public static <S, V> DataMapping<S, V, S, V> noSchemaId() {
        return new DataMapping<>() {
            @Override
            public Type<?, ?, ?> typeCheck(Type<?, ?, ?> type) {
                return null;
            }

            @Override
            public SchemaAndValue<S, V> transform(SchemaAndValue<S, V> schemaAndValue, Context context) {
                return new SchemaAndValue<>(NoSchema.INSTANCE, schemaAndValue.schema(), schemaAndValue.value());
            }
        };
    }
}
