/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.List;

/**
 * Persistent record mapping a subschema (identified by pointer) to its corresponding Java class name
 * and a mapping of the schema {@code properties} to the Java accessor method names.
 *
 * This is used by the compiler to support incremental compilation.
 * E.g. compile a schema, and later compile (possibly with a different version of the compiler)
 * another schema which {@code $ref}s the first.
 * The CodeGen needs to know what type names to use for those {@code $ref}s.
 * Likewise, it needs to know accessor names to support {@code CodeGen.mkMapDeserializer(String, SchemaObject, String, SchemaObject)}.
 * @param pointer A pointer to a subschema
 * @param classname The fully qualified java class name of the class corresponding to that schema
 * @param properties Mapping from schema property name to Java accessor method name.
 */
public record TypeModel(
        String pointer,
        @JsonProperty("class") String classname,
        List<PropertyModel> properties
) {

    public static final TypeModel UNKNOWN = new TypeModel(null, null, null);

    public String getDescription() {
    }

    public ClassOrInterfaceType type() {
        return (ClassOrInterfaceType) StaticJavaParser.parseType(classname);
    }

    record PropertyModel(
                         String propertyName,
                         String type,
                         String accessorName
                         ) {

    }
}
