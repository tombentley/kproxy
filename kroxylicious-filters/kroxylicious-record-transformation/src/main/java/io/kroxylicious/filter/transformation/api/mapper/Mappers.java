/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.api.schema.identification.NoSchema;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public class Mappers {
    private Mappers() {
    }

    static <S, T, U> Mapper<S, U> compose(Mapper<S, T> first, Mapper<T, U> andThen) {
        if (!andThen.acceptedType().isAssignableFrom(first.returnedType())) {
            throw new RuntimeException();
        }
        return new Mapper<>() {
            @Override
            public Class<S> acceptedType() {
                return first.acceptedType();
            }

            @Override
            public Class<U> returnedType() {
                return andThen.returnedType();
            }

            @Override
            public U transform(S value, Context context) {
                var intermediateResult = first.transform(value, context);
                return andThen.transform(intermediateResult, context);
            }
        };
    }

    public static Mapper<?, ?> compose(List<Mapper<?, ?>> mappers) {
        if (mappers.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (mappers.size() == 1) {
            return mappers.get(0);
        }

        Class<?> type = null;
        Mapper<?, ?> prevMapper = null;
        for (var mapper : mappers) {
            if (type != null
                    && !mapper.acceptedType().isAssignableFrom(type)) {
                throw new IllegalArgumentException(
                        "The mapper of type " + mapper.getClass().getName() + " cannot accept values of type " + type.getName() + " returned from the mapper of type " + prevMapper.getClass().getName()
                );
            }
            type = mapper.returnedType();
            prevMapper = mapper;
        }
        return new Mapper<>() {

            @Override
            public Class acceptedType() {
                return mappers.get(0).acceptedType();
            }

            @Override
            public Class returnedType() {
                return mappers.get(mappers.size() - 1).returnedType();
            }

            @Override
            public Object transform(Object value, Context context) {
                for (var mapper : mappers) {
                    value = applyTransform(mapper, value, context);
                }
                return value;
            }

            private static <S, T> Object applyTransform(Mapper<S, T> mapper, Object value, Context context) {
                S value1 = mapper.acceptedType().cast(value);
                return mapper.transform(value1, context);
            }
        };
    }

    public static <T> Mapper<List<T>, List<T>> emptyList() {
        return new Mapper<List<T>, List<T>>() {
            @Override
            public Class<List<T>> acceptedType() {
                return (Class) List.class;
            }

            @Override
            public Class<List<T>> returnedType() {
                return (Class) List.class;
            }

            @Override
            public List<T> transform(List<T> value, Context context) {
                return List.of();
            }
        };
    }

    public static Mapper<List<Header>, List<Header>> identityHeaders() {
        return Mappers.<List<Header>>identity((Class) List.class);
    }

    /**
     * Returns an instance of the identity mapper for the given type
     * @param type The Class of the type of this mapper
     * @return The identity mapper for the given type.
     * @param <T> The type of this mapper
     */
    public static <T> Mapper<T, T> identity(Class<T> type) {
        return new Mapper<>() {
            @Override
            public Class<T> acceptedType() {
                return type;
            }

            @Override
            public Class<T> returnedType() {
                return type;
            }

            @Override
            public T transform(T value, Context context) {
                return value;
            }
        };
    }

    public static <S extends WireSchemaId> Mapper<S, ? super S> preserve(Class<S> type) {
        return new Mapper<>() {
            @Override
            public Class<S> acceptedType() {
                return type;
            }

            @Override
            public Class<S> returnedType() {
                return type;
            }

            @Override
            public S transform(S wireSchemaId, Context context) {
                return wireSchemaId;
            }
        };
    }

    public static Mapper<WireSchemaId, NoSchema> noSchemaId() {
        return new Mapper<>() {
            @Override
            public Class<WireSchemaId> acceptedType() {
                return WireSchemaId.class;
            }

            @Override
            public Class<NoSchema> returnedType() {
                return NoSchema.class;
            }

            @Override
            public NoSchema transform(WireSchemaId wireSchemaId, Context context) {
                return NoSchema.INSTANCE;
            }
        };
    }
}
