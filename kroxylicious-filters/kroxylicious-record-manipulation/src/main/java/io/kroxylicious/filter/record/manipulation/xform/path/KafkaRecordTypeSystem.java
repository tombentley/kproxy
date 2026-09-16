/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.List;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.internal.Record;

public class KafkaRecordTypeSystem implements TypeSystem<Type> {
    @Override
    public Type topType() {
        return null;
    }

    @Override
    public boolean isUnionType(Type schema) {
        return false;
    }

    @Override
    public boolean isObjectType(Type schema) {
        return schema == Record.class || schema == Header.class;
    }

    @Override
    public boolean isObjectOpen(Type objectSchema) {
        return false;
    }

    @Override
    public List<String> objectProperties(Type objectSchema) {
        if (objectSchema == Record.class) {
            return List.of("timestamp", "sequence", "headers", "offset", "key", "value");
        }
        else if (objectSchema == Header.class) {
            return List.of("key", "value");
        }
        return List.of();
    }

    @Override
    public Type objectPropertySchema(Type objectSchema, String propertyName) {
        if (objectSchema == Record.class) {
            return switch (propertyName) {
                case "offset", "timestamp" -> Long.TYPE;
                case "sequence" -> Integer.TYPE;
                case "headers" -> Header[].class;
                case "key", "value" -> ByteBuffer.class;
                default -> null;
            };
        }
        else if (objectSchema == Header.class) {
            return switch (propertyName) {
                case "key" -> String.class;
                case "value" -> byte[].class;
                default -> null;
            };
        }
        return null;
    }

    @Override
    public Type objectPropertySchema(Type objectSchema) {
        if (objectSchema == Record.class) {
            return Union.of(List.of(Long.TYPE, Integer.TYPE, Header[].class, ByteBuffer.class));
        }
        else if (objectSchema == Header.class) {
            return Union.of(String.class, byte[].class);
        }
        return null;
    }

    @Override
    public boolean isArrayType(Type schema) {
        return schema == Header[].class;
    }

    @Override
    public boolean isArrayOpen(Type arraySchema) {
        return false;
    }

    @Override
    public int[] arrayIndexes(Type arraySchema) {
        return new int[0];
    }

    @Override
    public Type arrayItemSchema(Type arraySchema, int index) {
        return Header.class;
    }

    @Override
    public Type arrayItemSchema(Type arraySchema) {
        return Header.class;
    }

    @Override
    public Type bottomType() {
        return null;
    }

    @Override
    public Type typeOf(Type schema) {
        return schema;
    }
}
