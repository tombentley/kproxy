/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.neo.type;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Pairs a foreign schema {@code S} with the {@link TypeSystem} that interprets it, giving a single
 * navigable "type context". This is the value the type-checker threads while typing a path: navigation
 * methods ({@link #property(String)}, {@link #item(int)}, …) yield the {@code SchemaType} of a nested
 * value, and {@link #javaType()} converts the current position to the Java {@link Type} used for
 * assignability checks.
 * <p>
 * An operation that reinterprets its input under a new type system (e.g. a deserializer or a schema
 * validator) advertises the {@code SchemaType} it introduces, capturing both the interpreter and the
 * (already-parsed) schema in one wildcard-usable value.
 *
 * @param <S> The type of schema object (e.g. {@code Schema} for Avro, {@code JsonNode} for JSON Schema,
 * or {@link Type} for the Kafka record type system).
 */
public final class SchemaType<S> {

    private final TypeSystem<S> typeSystem;
    private final S schema;

    private SchemaType(TypeSystem<S> typeSystem, S schema) {
        this.typeSystem = typeSystem;
        this.schema = schema;
    }

    public static <S> SchemaType<S> of(TypeSystem<S> typeSystem, S schema) {
        return new SchemaType<>(typeSystem, schema);
    }

    /** The Java type of a value at this position (the terminal conversion once a path is fully navigated). */
    public Type javaType() {
        return typeSystem.typeOf(schema);
    }

    /** The most general type of the type system, used when navigation cannot be narrowed (e.g. descendant segments). */
    public Type topType() {
        return typeSystem.topType();
    }

    public boolean isObject() {
        return typeSystem.isObjectType(schema);
    }

    public boolean isObjectOpen() {
        return typeSystem.isObjectOpen(schema);
    }

    public List<String> propertyNames() {
        return typeSystem.objectProperties(schema);
    }

    public SchemaType<S> property(String name) {
        return of(typeSystem, typeSystem.objectPropertySchema(schema, name));
    }

    public SchemaType<S> anyProperty() {
        return of(typeSystem, typeSystem.objectPropertySchema(schema));
    }

    public boolean isArray() {
        return typeSystem.isArrayType(schema);
    }

    public boolean isArrayOpen() {
        return typeSystem.isArrayOpen(schema);
    }

    public int[] itemIndexes() {
        return typeSystem.arrayIndexes(schema);
    }

    public SchemaType<S> item(int index) {
        return of(typeSystem, typeSystem.arrayItemSchema(schema, index));
    }

    public SchemaType<S> anyItem() {
        return of(typeSystem, typeSystem.arrayItemSchema(schema));
    }
}
