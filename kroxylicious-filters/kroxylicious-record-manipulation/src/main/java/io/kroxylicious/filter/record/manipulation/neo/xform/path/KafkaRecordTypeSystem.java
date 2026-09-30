/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.neo.xform.path;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.List;

import io.kroxylicious.filter.record.manipulation.neo.type.Bottom;
import io.kroxylicious.filter.record.manipulation.neo.type.TypeSystem;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.internal.Record;

public class KafkaRecordTypeSystem implements TypeSystem<Type> {
    @Override
    public Type topType() {
        // Any value reachable within a record.
        return Object.class;
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
                // TODO key/value are really nullable (a tombstone value, or an absent key), i.e.
                // Union.of(ByteBuffer.class, Null.class). Modelling that honestly requires union-aware
                // assignability in TypeCheckingVisitor (GenericTypeReflector cannot handle our Union) and
                // null-tolerant ops; deferred. For now they are typed as the present (non-null) value.
                case "key", "value" -> ByteBuffer.class;
                default -> Bottom.class;
            };
        }
        else if (objectSchema == Header.class) {
            return switch (propertyName) {
                case "key" -> String.class;
                case "value" -> byte[].class;
                default -> Bottom.class;
            };
        }
        return Bottom.class;
    }

    @Override
    public Type objectPropertySchema(Type objectSchema) {
        // Records and headers are closed objects; there is no "unknown property" value.
        return Bottom.class;
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
        return Bottom.class;
    }

    @Override
    public Type typeOf(Type schema) {
        return schema;
    }
}
