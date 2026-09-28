/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Pairs a foreign schema {@code S} with the {@link TypeSystem} that interprets it, giving a single
 * navigable "type context". This is the value that {@link TypeCheckingVisitor} threads while typing
 * a path: navigation methods ({@link #property(String)}, {@link #item(int)}, …) yield the
 * {@code SchemaType} of a nested value, and {@link #javaType()} converts the current position to the
 * Java {@link Type} used for assignability checks.
 *
 * @param <S> The type of schema object (e.g. {@code Schema} for Avro, {@code JsonNode} for JSON Schema,
 * or {@link Type} for the Kafka record type system).
 */
final class SchemaType<S> {

    private final TypeSystem<S> typeSystem;
    private final S schema;

    private SchemaType(TypeSystem<S> typeSystem, S schema) {
        this.typeSystem = typeSystem;
        this.schema = schema;
    }

    static <S> SchemaType<S> of(TypeSystem<S> typeSystem, S schema) {
        return new SchemaType<>(typeSystem, schema);
    }

    /** The Java type of a value at this position (the terminal conversion once a path is fully navigated). */
    Type javaType() {
        return typeSystem.typeOf(schema);
    }

    /** The most general type of the type system, used when navigation cannot be narrowed (e.g. descendant segments). */
    Type topType() {
        return typeSystem.topType();
    }

    boolean isObject() {
        return typeSystem.isObjectType(schema);
    }

    boolean isObjectOpen() {
        return typeSystem.isObjectOpen(schema);
    }

    List<String> propertyNames() {
        return typeSystem.objectProperties(schema);
    }

    SchemaType<S> property(String name) {
        return of(typeSystem, typeSystem.objectPropertySchema(schema, name));
    }

    SchemaType<S> anyProperty() {
        return of(typeSystem, typeSystem.objectPropertySchema(schema));
    }

    boolean isArray() {
        return typeSystem.isArrayType(schema);
    }

    boolean isArrayOpen() {
        return typeSystem.isArrayOpen(schema);
    }

    int[] itemIndexes() {
        return typeSystem.arrayIndexes(schema);
    }

    SchemaType<S> item(int index) {
        return of(typeSystem, typeSystem.arrayItemSchema(schema, index));
    }

    SchemaType<S> anyItem() {
        return of(typeSystem, typeSystem.arrayItemSchema(schema));
    }
}
