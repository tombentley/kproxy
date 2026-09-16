/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

import io.leangen.geantyref.TypeFactory;

public class AvroGenericTypeSystem implements TypeSystem<Schema> {
    @Override
    public Type topType() {
        return null;
    }

//    @Override
//    public Type unionType(List<Type> caseTypes) {
//        return null;
//    }

//    @Override
//    public Set<Type> caseTypes(Schema unionSchema) {
//        return Set.of();
//    }

    @Override
    public boolean isUnionType(Schema schema) {
        return schema.getType() == Schema.Type.UNION;
    }

    @Override
    public boolean isObjectType(Schema schema) {
        Schema.Type type = schema.getType();
        return type == Schema.Type.MAP
                || type == Schema.Type.RECORD;
    }

    @Override
    public boolean isObjectOpen(Schema objectSchema) {
        return objectSchema.getType() == Schema.Type.MAP;
    }

    @Override
    public List<String> objectProperties(Schema objectSchema) {
        if (objectSchema.getType() == Schema.Type.RECORD) {
            return objectSchema.getFields().stream().map(Schema.Field::name).toList();
        }
        return List.of();
    }

    @Override
    public Schema objectPropertySchema(Schema objectSchema, String propertyName) {
        if (objectSchema.getType() == Schema.Type.RECORD) {
            Schema.Field field = objectSchema.getField(propertyName);
            if (field != null) {
                return field.schema();
            }
            return null;
        }
        else if (objectSchema.getType() == Schema.Type.MAP) {
            return objectSchema.getValueType();
        }
        return null;
    }

    @Override
    public Schema objectPropertySchema(Schema objectSchema) {
        if (objectSchema.getType() == Schema.Type.MAP) {
            return objectSchema.getValueType();
        }
        return null;
    }

    @Override
    public boolean isArrayType(Schema schema) {
        return schema.getType() == Schema.Type.ARRAY;
    }

    @Override
    public boolean isArrayOpen(Schema arraySchema) {
        if (arraySchema.getType() == Schema.Type.ARRAY) {
            return true;
        }
        return false;
    }

    @Override
    public int[] arrayIndexes(Schema arraySchema) {
        if (arraySchema.getType() == Schema.Type.ARRAY) {
            return new int[0];
        }
        return null;
    }

    @Override
    public Schema arrayItemSchema(Schema arraySchema, int index) {
        return arraySchema.getElementType();
    }

    @Override
    public Schema arrayItemSchema(Schema arraySchema) {
        return arraySchema.getElementType();
    }

    @Override
    public Type bottomType() {
        return Bottom.class;
    }

    record Bottom() {}

    @Override
    public Type typeOf(Schema schema) {
        return switch (schema.getType()) {
            case RECORD -> GenericRecord.class;
            case INT ->  Integer.class;
            case LONG -> Long.class;
            case DOUBLE -> Double.class;
            case FLOAT -> Float.class;
            case BOOLEAN -> Boolean.class;
            case NULL -> Void.class; // TODO ???
            case STRING -> String.class;
            case BYTES -> byte[].class;
            case ENUM -> String.class; // TODO ???
            case FIXED -> byte[].class;
            case ARRAY -> TypeFactory.parameterizedClass(List.class, typeOf(schema.getElementType()));
            case MAP -> TypeFactory.parameterizedClass(Map.class, String.class, typeOf(schema.getValueType()));
            case UNION -> Union.of(schema.getTypes().stream().map(this::typeOf).toList());
        };
    }
}
