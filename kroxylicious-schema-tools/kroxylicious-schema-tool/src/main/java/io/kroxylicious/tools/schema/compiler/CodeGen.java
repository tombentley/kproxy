/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.ast.type.VoidType;

import io.kroxylicious.tools.schema.model.SchemaObject;
import io.kroxylicious.tools.schema.model.SchemaType;
import io.kroxylicious.tools.schema.model.SchemaVisitor;
import io.kroxylicious.tools.schema.model.XKubeListType;

import edu.umd.cs.findbugs.annotations.NonNull;

import static com.github.javaparser.ast.Modifier.createModifierList;

/**
 * Java code generation from a SchemaObject.
 */
public class CodeGen {

    public static final String UNKNOWN_PROPERTIES_FIELD_NAME = "unknownProperties";
    private final IdVisitor idVisitor;
    private final Diagnostics diagnostics;
    private final Map<String, String> existingClasses;
    private final PropertyStrategy propertyStrategy;
    private final String nullableAnnotation;
    private final String nonNullAnnotation;
    private final List<TypeAnnotator> typeAnnotators;
    private final List<PropertyAnnotator> propertyAnnotators;
    private final boolean optAccessor;
    private final Catalog catalog;

    public CodeGen(Diagnostics diagnostics,
                   IdVisitor idVisitor,
                   Catalog catalog,
                   Map<String, String> existingClasses,
                   String nullableAnnotation,
                   String nonNullAnnotation,
                   List<TypeAnnotator> typeAnnotators,
                   PropertyStrategy propertyStrategy,
                   boolean optAccessor,
                   List<PropertyAnnotator> propertyAnnotators) {
        this.diagnostics = Objects.requireNonNull(diagnostics);
        this.idVisitor = Objects.requireNonNull(idVisitor);
        this.catalog = Objects.requireNonNull(catalog);
        this.existingClasses = existingClasses;
        this.propertyStrategy = propertyStrategy;
        this.optAccessor = optAccessor;
        this.nullableAnnotation = nullableAnnotation;
        this.nonNullAnnotation = nonNullAnnotation;
        this.typeAnnotators = typeAnnotators;
        this.propertyAnnotators = propertyAnnotators;
    }

    SchemaObject resolveRef(SchemaObject root, SchemaObject schema) {
        var ref = schema.getRef() == null ? null : URI.create(schema.getRef());
        if (ref != null) {

            if ((ref.getPath() == null
                    || ref.getPath().isEmpty())
                    && ref.getFragment() != null) {
                return resolveInternalFragmentRef(root, ref);
            }
            // Two possibilities: a local ref point to a file which we should be compiling
            // or a local ref pointing to a file which we should _not be compiling_
            // In the _not compiling_ case the referring file depends on decls which we won't emit
            // If the java for the referred to file is out of date then we'll likely generate the wrong thing

            URI sought = URI.create(root.getId()).resolve(ref);
            var resolved = idVisitor.resolve(sought);
            if (resolved != null) {
                return resolved;
            }
            var resolved2 = catalog.lookup(sought);
            if (resolved2 != null) {
                // TODO this is such a hack! We're constructing a minimal SchemaObject
                //  from the type model. What we should do is change this method to return a type model
                //  that would avoid needing to instantiate any SchemaObject and make more explicit the idea
                //  that refs are models
                SchemaObject schemaObject = new SchemaObject();
                schemaObject.setType(List.of(SchemaType.OBJECT));
                schemaObject.setUnknownProperty("$$model", resolved2);
                return schemaObject;
            }

            diagnostics.reportError("Cannot resolve $ref (but $ref not fully supported) {}", ref);
            return new SchemaObject();

        }
        else {
            return schema;
        }
    }

    @NonNull
    private SchemaObject resolveInternalFragmentRef(SchemaObject root, URI ref) {
        if (ref.getFragment().startsWith("/definitions/")) {
            Map<String, SchemaObject> defs = root.getDefinitions();
            if (defs != null) {
                String name = ref.getFragment().substring("/definitions/".length());
                SchemaObject object = defs.get(name);
                if (object != null) {
                    return object;
                }
            }
            diagnostics.reportFatal("Couldn't resolve $ref " + ref);
            return new SchemaObject();
        }
        diagnostics.reportFatal("$ref not fully supported");
        return new SchemaObject();
    }

    @SuppressWarnings("java:S1192")
    ClassOrInterfaceType genTypeName(String pkg, SchemaObject root, SchemaObject schemaOrRef) {
        Objects.requireNonNull(schemaOrRef);
        var schema = resolveRef(root, schemaOrRef);
        List<SchemaType> type = schema.getType();
        if (type == null || type.isEmpty()) {
            // unconstrained => union of all types
            type = new ArrayList<>(SchemaType.all());
        }
        else {
            type = new ArrayList<>(type);
        }
        if (type.size() == 1) {
            return switch (type.get(0)) {
                case NULL -> mkType("java.lang.Object");
                case BOOLEAN -> mkType("java.lang.Boolean");
                case INTEGER -> mkType("java.lang.Long");
                case NUMBER -> mkType("java.lang.Double");
                case STRING -> {
                    if (schema.getFormat() != null) {
                        yield mkType(switch (schema.getFormat()) {
                            case "uri" -> "java.net.URI";
                            default -> "java.lang.String";
                        });
                    }
                    else {
                        yield mkType("java.lang.String");
                    }
                }
                case ARRAY -> genCollectionOrMapType(pkg, root, schema);
                case OBJECT -> {
                    if (mapObjectAsMap(schema)) {
                        yield mkGenericType("java.util.Map", mkType("java.lang.String"),
                                genTypeName(pkg, root, schema.getAdditionalProperties().getSchemaObject()));
                    }
                    TypeModel typeModel = (TypeModel) schema.getUnknownProperties().get("$$model");
                    if (typeModel != null) {
                        yield mkType(typeModel.pkg() + "." + typeModel.classname());
                    }
                    else {
                        // TODO or Map or ObjectNode if x-kubernetes-preserve-unknown-keys
                        String fqName = pkg + "." + className(schema);
                        String orDefault = existingClasses.getOrDefault(fqName, fqName);
                        yield mkType(orDefault);
                    }
                }
            };
        }
        else {
            return mkType("java.lang.Object");
        }
    }

    @NonNull
    private ClassOrInterfaceType genCollectionOrMapType(String pkg, SchemaObject root, SchemaObject schema) {
        var itemType = genTypeName(pkg, root, schema.getItems().get(0));
        XKubeListType xKubeListType = schema.getXKubernetesListType();
        if (xKubeListType == null
                || xKubeListType == XKubeListType.ATOMIC) {
            return mkGenericType("java.util.List", itemType);
        }
        else if (xKubeListType == XKubeListType.SET) {
            return mkGenericType("java.util.Set", itemType);
        }
        else if (xKubeListType == XKubeListType.MAP) {
            return mkGenericType("java.util.Map",
                    genMapKeyType(pkg, root, schema, schema.getItems().get(0)),
                    itemType);
        }
        else {
            diagnostics.reportError("Unsupported 'x-kubernetes-list-type': " + xKubeListType);
            return genErrorType();
        }
    }

    @NonNull
    private Type genMapKeyType(String pkg, SchemaObject root, SchemaObject schema, SchemaObject itemSchemaOrRef) {
        List<String> keyPropertyNames = schema.getXKubernetesListMapKeys();
        Type keyType;
        if (keyPropertyNames == null
                || keyPropertyNames.isEmpty()) {
            diagnostics.reportError("'x-kubernetes-list-map-keys' property is required when 'x-kubernetes-list-type: map'");
            // Use some type so we can keep going, even though the Java won't compile
            keyType = genErrorType();
        }
        else if (keyPropertyNames.size() > 1) {
            // x-kubernetes-list-map-keys=['foo', 'bar'] should result in an inner class to represent the compound key
            diagnostics.reportError("'x-kubernetes-list-map-keys' property with multiple values is not yet supported");
            // Use some type so we can keep going, even though the Java won't compile
            keyType = genErrorType();
        }
        else {
            SchemaObject keySchema = resolveRef(root, itemSchemaOrRef).getProperties().get(keyPropertyNames.get(0));
            keyType = genTypeName(pkg, root, keySchema);
        }
        return keyType;
    }

    @NonNull
    private String genMapKeyAccessor(String pkg, SchemaObject root, SchemaObject schema, SchemaObject itemSchema) {
        List<String> keyPropertyNames = schema.getXKubernetesListMapKeys();
        if (keyPropertyNames.size() == 1) {
            return propertyStrategy.accessorName(keyPropertyNames.get(0));
        }
        return "???"; // error should have been reported when generating the type
    }

    /**
     * Sometimes it's better to generate a type, even in the presence of an invalid schema,
     * so we can at least generate some java code and report more errors to the user.
     * @return An "error type"
     */
    @NonNull
    private static ClassOrInterfaceType genErrorType() {
        return mkType("code.generation.Error");
    }

    List<Unit> genDecls(SchemaInput input) {
        var result = new ArrayList<Unit>();
        // TODO visit the subschemas given them javaType names if they don't have them already.
        // If loaded from URI ending /x or /x.yaml or /X or /X.yaml
        // root schema = X
        // definions = the name
        // subschema of root via property foo = XFoo
        // subschema of root via item of array foos = XFoo
        input.visitSchemas(diagnostics, new CodeGenVisitor(input, result));
        return result;
    }

    /**
     * Generate a type declaration for the given schema, or null if the type is declared externally
     *
     * @param pkg
     * @param schema
     */
    private @Nullable Unit genDecl(String pkg, SchemaObject schema, String path, URI base) {
        List<SchemaType> type = schema.getType();
        if (type == null) {
            type = SchemaType.all();
        }
        if (type.size() == 1) {
            return switch (type.get(0)) {
                case OBJECT -> {
                    if (mapObjectAsMap(schema)) {
                        yield null;
                    }
                    else {
                        yield genClass(pkg, idVisitor.resolve(base), schema, path);
                    }
                }
                case ARRAY, STRING, INTEGER, NUMBER, BOOLEAN, NULL -> null;
            };
        }
        else {
            throw new UnsupportedOperationException("Can't handle `type`: " + type);
        }
    }

    private static boolean mapObjectAsMap(SchemaObject schema) {
        return (schema.getProperties() == null
                || schema.getProperties().isEmpty())
                && schema.getAdditionalProperties() != null
                && schema.getAdditionalProperties().getSchemaObject() != null;
    }

    public static boolean isTypeGenerated(SchemaObject schemaObject) {
        return List.of(SchemaType.OBJECT).equals(schemaObject.getType());
    }

    Map<String, String> seen = new HashMap<>();

    @Nullable
    private Unit genClass(String pkg, SchemaObject root, SchemaObject schema, String path) {
        assert (isTypeGenerated(schema));
        final var model = model(schema);
        final String className = model.classname();
        if (existingClasses.containsKey(pkg + "." + className)) {
            return null;
        }
        String oldPath = seen.put(className, path);
        if (oldPath != null) {
            diagnostics.reportFatal(
                    "Already generated {} when visited {}, now trying to generate it again when visiting {}",
                    className,
                    oldPath,
                    path);
        }
        Map<String, SchemaObject> properties = properties(schema);

        Set<String> required = schema.getRequired() == null ? Set.of() : schema.getRequired();
        CompilationUnit cu = new CompilationUnit();
        cu.setPackageDeclaration(new PackageDeclaration(new Name(pkg)));

        ClassOrInterfaceDeclaration clz = cu.addClass(className,
                Modifier.Keyword.PUBLIC);
        String classDescription = schema.getDescription();
        if (classDescription == null) {
            classDescription = "Auto-generated class representing the schema at " + path + ".";
        }
        clz.setJavadocComment(classDescription);

        // @javax.annotation.processing.Generated("...")
        clz.addAnnotation(new SingleMemberAnnotationExpr(
                new Name("javax.annotation.processing.Generated"),
                new StringLiteralExpr(getClass().getName())));
        // @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        clz.addAnnotation(new SingleMemberAnnotationExpr(
                new Name("com.fasterxml.jackson.annotation.JsonInclude"),
                new FieldAccessExpr(new TypeExpr(mkType("com.fasterxml.jackson.annotation.JsonInclude.Include")), "NON_NULL")));

        // @com.fasterxml.jackson.annotation.JsonPropertyOrder({...properties...})
        if (!properties.isEmpty()) {
            clz.addAnnotation(new SingleMemberAnnotationExpr(
                    new Name("com.fasterxml.jackson.annotation.JsonPropertyOrder"),
                    new ArrayInitializerExpr(new NodeList<>(properties.keySet().stream().map(x -> (Expression) new StringLiteralExpr(x)).toList()))));
        }
        // @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
        clz.addAnnotation(new NormalAnnotationExpr(new Name("com.fasterxml.jackson.databind.annotation.JsonDeserialize"),
                new NodeList<>(new MemberValuePair("using", new ClassExpr(mkType("com.fasterxml.jackson.databind.JsonDeserializer.None"))))));

        typeAnnotators.stream().flatMap(ta -> ta.annotateClass(diagnostics, schema).stream()).forEach(clz::addAnnotation);

        // Deserializer static inner classes
        for (var entry : properties.entrySet()) {
            String propName = entry.getKey();
            if (useMapDeserializer(root, entry.getValue())) {
                clz.addMember(mkMapDeserializer(pkg, root, propName, entry.getValue()));
                clz.addMember(mkMapSerializer(pkg, root, propName, entry.getValue()));
            }
        }

        // fields
        for (var entry : properties.entrySet()) {
            String propName = entry.getKey();
            var propType = genTypeName(pkg, root, entry.getValue());
            FieldDeclaration fieldDeclaration = mkPropertyField(schema, propName, propType);
            var propSchemaXX = resolveRef(root, entry.getValue());
            propertyAnnotators.stream().flatMap(ta -> ta.annotateField(diagnostics, propName, propSchemaXX).stream()).forEach(fieldDeclaration::addAnnotation);
            clz.addMember(fieldDeclaration);
        }
        if (additionalPropertiesNotFalse(schema)) {
            clz.addMember(mkUnknownPropertiesField());
        }

        // constructors
        mkConstructors(pkg, root, schema, properties, required, clz);

        // accessors and mutators
        for (var entry : properties.entrySet()) {
            String propName = entry.getKey();
            var propType = genTypeName(pkg, root, entry.getValue());
            clz.addMember(mkPropertyGetterMethod(pkg, root, schema, entry.getValue(), propName, propType));
            if (optAccessor) {

                clz.addMember(mkPropertyOptMethod(root, entry.getValue(), propName, propType, required.contains(propName)));
            }
            clz.addMember(mkPropertySetterMethod(root, schema, entry.getValue(), propName, propType));
        }

        if (additionalPropertiesNotFalse(schema)) {
            clz.addMember(mkUnknownPropertiesGetterMethod());
            clz.addMember(mkUnknownPropertiesSetterMethod());
        }

        // toString
        clz.addMember(mkToStringMethod(className, schema));
        // hashCode
        clz.addMember(mkHashCodeMethod(schema));
        // equals
        clz.addMember(mkEqualsMethod(pkg, className, schema));

        return new Unit(URI.create(root.getId()), cu, model);
    }

    /**
     * When a JSON Schema `array` type has x-kubernetes-list-type=map we generate
     * a Deserializer and Serializer static member class on the owning class according to the map keys.
     * We don't generate the @JsonDeserializer and @JsonSerializer annotations of the array item type
     * because in general the same type
     * could be referenced as the `items` type of multiple properties
     * (i.e. the properties which make up the key are defined on the "owner type",
     * not the array item type)
     * @param propSchemaOrRef A property type schema
     * @return true if the {@code propSchema} has type==array and x-kubernetes-list-type=map.
     * @see #mkMapDeserializer(String, SchemaObject, String, SchemaObject)
     * @see #mkMapSerializer(String, SchemaObject, String, SchemaObject)
     */
    private boolean useMapDeserializer(SchemaObject root, SchemaObject propSchemaOrRef) {
        var schema = resolveRef(root, propSchemaOrRef);
        return List.of(SchemaType.ARRAY).equals(schema.getType())
                && schema.getXKubernetesListType() == XKubeListType.MAP;
    }

    /**
     * Generate a Deserializer class for a Map-typed property.
     * We generate a complete class, without relying on a runtime library.
     * This means we only depend on {@code java.base} and Jackson.
     *
     * @see #useMapDeserializer(SchemaObject, SchemaObject)
     * @see #mkMapSerializer(String, SchemaObject, String, SchemaObject)
     */
    @NonNull
    private ClassOrInterfaceDeclaration mkMapSerializer(String pkg,
                                                        SchemaObject root,
                                                        String propName,
                                                        SchemaObject propSchema) {

        var mapValueType = genTypeName(pkg, root, propSchema.getItems().get(0));
        var mapKeyType = genMapKeyType(pkg, root, propSchema, propSchema.getItems().get(0));
        ClassOrInterfaceDeclaration serializerClass = new ClassOrInterfaceDeclaration(createModifierList(Modifier.Keyword.STATIC), false, serializerClassName(propName));
        ClassOrInterfaceType mapType = mkGenericType("java.util.Map", mapKeyType, mapValueType);
        serializerClass.addExtendedType(mkGenericType("com.fasterxml.jackson.databind.JsonSerializer",
                mapType));

        var serializeMethod = new MethodDeclaration();
        serializeMethod.addAnnotation(mkAtOverride());
        serializeMethod.setModifiers(Modifier.Keyword.PUBLIC);
        serializeMethod.setType(new VoidType());
        serializeMethod.setName("serialize");
        serializeMethod.addParameter(new Parameter(mapType, "map"));
        serializeMethod.addParameter(new Parameter(mkType("com.fasterxml.jackson.core.JsonGenerator"), "generator"));
        serializeMethod.addParameter(new Parameter(mkType("com.fasterxml.jackson.databind.SerializerProvider"), "provider"));
        serializeMethod.addThrownException(mkType("java.io.IOException"));
        serializeMethod.setBody(new BlockStmt(NodeList.nodeList(
                new ExpressionStmt(new MethodCallExpr("generator.writeStartArray")),
                new ForEachStmt(new VariableDeclarationExpr(new VarType(), "item"),
                        new MethodCallExpr("map.values"),
                        new ExpressionStmt(new MethodCallExpr("generator.writeObject", new NameExpr("item")))),
                new ExpressionStmt(new MethodCallExpr("generator.writeEndArray")))));

        serializerClass.addMember(serializeMethod);
        return serializerClass;
    }

    @NonNull
    private ClassOrInterfaceDeclaration mkMapDeserializer(String pkg,
                                                          SchemaObject root,
                                                          String propName,
                                                          SchemaObject propSchema) {
        var mapValueType = genTypeName(pkg, root, propSchema.getItems().get(0));

        var mapKeyType = genMapKeyType(pkg, root, propSchema, propSchema.getItems().get(0));
        ClassOrInterfaceDeclaration deserializerClass = new ClassOrInterfaceDeclaration(createModifierList(Modifier.Keyword.STATIC), false,
                deserializerClassName(propName));
        ClassOrInterfaceType mapType = mkGenericType("java.util.Map", mapKeyType, mapValueType);
        deserializerClass.addExtendedType(mkGenericType("com.fasterxml.jackson.databind.JsonDeserializer",
                mapType));

        var deserializeMethod = new MethodDeclaration();
        deserializeMethod.addAnnotation(mkAtOverride());
        deserializeMethod.setModifiers(Modifier.Keyword.PUBLIC);
        deserializeMethod.setType(mapType);
        deserializeMethod.setName("deserialize");
        deserializeMethod.addParameter(new Parameter(mkType("com.fasterxml.jackson.core.JsonParser"), "parser"));
        deserializeMethod.addParameter(new Parameter(mkType("com.fasterxml.jackson.databind.DeserializationContext"), "context"));
        deserializeMethod.addThrownException(mkType("java.io.IOException"));
        deserializeMethod.setBody(new BlockStmt(NodeList.nodeList(
                new ExpressionStmt(new VariableDeclarationExpr(
                        new VariableDeclarator(
                                mkType("com.fasterxml.jackson.databind.ObjectMapper"),
                                "mapper",
                                new CastExpr(
                                        mkType("com.fasterxml.jackson.databind.ObjectMapper"),
                                        new MethodCallExpr("parser.getCodec"))))),
                new ExpressionStmt(new VariableDeclarationExpr(
                        new VariableDeclarator(
                                mkGenericType("java.util.List", mapValueType),
                                "list",
                                new MethodCallExpr("mapper.readValue",
                                        new NameExpr("parser"),
                                        new ObjectCreationExpr(
                                                null,
                                                mkGenericType("com.fasterxml.jackson.core.type.TypeReference",
                                                        mkGenericType("java.util.List", mapValueType)),
                                                NodeList.nodeList(),
                                                NodeList.nodeList(), // args
                                                new NodeList<>(/* anonymous class body */)))))),
                new ExpressionStmt(new VariableDeclarationExpr(
                        new VariableDeclarator(
                                mkGenericType("java.util.Map", mapKeyType, mapValueType),
                                "result",
                                new ObjectCreationExpr(null, mkGenericType("java.util.LinkedHashMap", new String[0]), NodeList.nodeList())))), // TODO mapper.readValue
                new ForEachStmt(new VariableDeclarationExpr(new VarType(), "item"), new NameExpr("list"),
                        new ExpressionStmt(new MethodCallExpr("result.put",
                                new MethodCallExpr("item." + genMapKeyAccessor(pkg, root, propSchema, propSchema.getItems().get(0))),
                                new NameExpr("item")))),
                new ReturnStmt(new NameExpr("result")))));

        deserializerClass.addMember(deserializeMethod);
        return deserializerClass;
    }

    @NonNull
    private static String deserializerClassName(String propName) {
        return deriveClassNameForProperty(propName, "Deserializer");
    }

    @NonNull
    private static String serializerClassName(String propName) {
        return deriveClassNameForProperty(propName, "Serializer");
    }

    @NonNull
    private static String deriveClassNameForProperty(String propName, String suffix) {
        String s = fieldName(propName);
        int codepoint = s.codePointAt(0);
        int count = Character.charCount(codepoint);
        return s.substring(0, count).toUpperCase(Locale.ROOT) + s.substring(count) + suffix;
    }

    @NonNull
    private static Map<String, SchemaObject> properties(SchemaObject schema) {
        return schema.getProperties() == null ? Map.of() : schema.getProperties();
    }

    private static boolean additionalPropertiesNotFalse(SchemaObject schema) {
        return schema.getAdditionalProperties() == null
                || !Boolean.FALSE.equals(schema.getAdditionalProperties().getBooleanValue());
    }

    private MethodDeclaration mkUnknownPropertiesGetterMethod() {
        // TODO require jackson annotation
        String description = """
                Get any additional properties not declared in the schema.
                @return value The properties.
                """;
        MethodDeclaration methodDeclaration = new MethodDeclaration();
        methodDeclaration.setJavadocComment(description);
        // propertyAnnotators.stream().flatMap(ta -> ta.annotateMutator(diagnostics, propName, propSchema).stream()).forEach(methodDeclaration::addAnnotation);
        methodDeclaration.setModifiers(Modifier.Keyword.PUBLIC);
        methodDeclaration.setType(mkGenericType("java.util.Map", "java.lang.String", "java.lang.Object"));
        methodDeclaration.addAnnotation(mkNullableAnnotation(true));
        methodDeclaration.addAnnotation(mkAtJsonAnyGetter());
        methodDeclaration.setName("getAdditionalProperties");
        // propertyAnnotators.stream().flatMap(ta -> ta.annotateMutatorParameter(diagnostics, propName, propSchema).stream()).forEach(parameter::addAnnotation);
        methodDeclaration
                .setBody(new BlockStmt(new NodeList<>(
                        new ReturnStmt(new ConditionalExpr(
                                new BinaryExpr(new FieldAccessExpr(new ThisExpr(), UNKNOWN_PROPERTIES_FIELD_NAME), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
                                new MethodCallExpr(new NameExpr("java.util.Map"), "of", NodeList.nodeList()),
                                new FieldAccessExpr(new ThisExpr(), UNKNOWN_PROPERTIES_FIELD_NAME))))));
        return methodDeclaration;
    }

    private static ClassOrInterfaceType mkType(String base) {
        return new ClassOrInterfaceType(null, base);
    }

    @NonNull
    private static ClassOrInterfaceType mkGenericType(String base, String... typeArgs) {
        var typeArgsTypes = Arrays.stream(typeArgs).<Type> map(n -> new ClassOrInterfaceType(null, n)).toList();
        return mkGenericType(base, typeArgsTypes);
    }

    private static ClassOrInterfaceType mkGenericType(String base, Type... typeArgs) {
        return mkGenericType(base, Arrays.asList(typeArgs));
    }

    private static ClassOrInterfaceType mkGenericType(String base, List<Type> typeArgs) {
        return new ClassOrInterfaceType(null, new SimpleName(base), NodeList.nodeList(typeArgs));
    }

    private MethodDeclaration mkUnknownPropertiesSetterMethod() {
        // TODO require jackson annotation
        String description = """
                Add an additional property not declared in the schema.
                @param name The name of the property.
                @param value The value of the property.
                """;
        MethodDeclaration methodDeclaration = new MethodDeclaration();
        methodDeclaration.setJavadocComment(description);
        // propertyAnnotators.stream().flatMap(ta -> ta.annotateMutator(diagnostics, propName, propSchema).stream()).forEach(methodDeclaration::addAnnotation);
        methodDeclaration.addAnnotation(mkAtJsonAnySetter());
        methodDeclaration.setModifiers(Modifier.Keyword.PUBLIC);
        methodDeclaration.setType(new VoidType());
        methodDeclaration.setName("setAdditionalProperty");
        Parameter nameParameter = new Parameter(mkType("java.lang.String"), "name").addAnnotation(mkNullableAnnotation(true));
        Parameter valueParameter = new Parameter(mkType("java.lang.Object"), "value").addAnnotation(mkNullableAnnotation(true));
        // propertyAnnotators.stream().flatMap(ta -> ta.annotateMutatorParameter(diagnostics, propName, propSchema).stream()).forEach(parameter::addAnnotation);
        methodDeclaration
                .setParameters(new NodeList<>(nameParameter, valueParameter))
                .setBody(new BlockStmt(new NodeList<>(
                        new ExpressionStmt(new MethodCallExpr("java.util.Objects.requireNonNull", new NameExpr("name"))),
                        new IfStmt(new BinaryExpr(new FieldAccessExpr(new ThisExpr(), UNKNOWN_PROPERTIES_FIELD_NAME), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
                                new ExpressionStmt(new AssignExpr(new FieldAccessExpr(new ThisExpr(), UNKNOWN_PROPERTIES_FIELD_NAME),
                                        new ObjectCreationExpr(null, mkGenericType("java.util.HashMap", new String[0]), // diamond
                                                NodeList.nodeList()),
                                        AssignExpr.Operator.ASSIGN)),
                                null),
                        new ExpressionStmt(new MethodCallExpr(new FieldAccessExpr(new ThisExpr(), UNKNOWN_PROPERTIES_FIELD_NAME), "put",
                                NodeList.nodeList(new NameExpr("name"), new NameExpr("value")))))));

        return methodDeclaration;
    }

    private void mkConstructors(String pkg,
                                SchemaObject root,
                                SchemaObject object,
                                Map<String, SchemaObject> properties,
                                Set<String> required,
                                ClassOrInterfaceDeclaration clz) {

        // Add the all properties ctor
        ConstructorDeclaration decl = mkConstructor(pkg, root, object, clz,
                "All properties constructor.", properties, required,
                (propName, schemaObject) -> Stream.concat(
                        Stream.of(mkAtJsonProperty(propName, required.contains(propName))),
                        propertyAnnotators.stream()
                                .flatMap(ta -> ta.annotateConstructorParameter(diagnostics, propName, schemaObject).stream()))
                        .toList());
        decl.addAnnotation(new MarkerAnnotationExpr("com.fasterxml.jackson.annotation.JsonCreator"));
        clz.addMember(decl);

    }

    @NonNull
    private ConstructorDeclaration mkConstructor(String pkg,
                                                 SchemaObject root,
                                                 SchemaObject schema,
                                                 ClassOrInterfaceDeclaration clz,
                                                 String javadoc,
                                                 Map<String, SchemaObject> properties,
                                                 Set<String> required,
                                                 BiFunction<String, SchemaObject, List<AnnotationExpr>> annotator) {
        ConstructorDeclaration ctor = new ConstructorDeclaration();

        ctor.setJavadocComment(properties.keySet().stream()
                .map(propName -> {
                    String s = "@param " + fieldName(propName) + " The value of the {@code " + propName + "} property.";
                    if (required.contains(propName)) {
                        s += " This is a required property.";
                    }
                    else {
                        s += " This is an optional property.";
                    }
                    return s;
                })
                .collect(Collectors.joining("\n", javadoc + "\n", "")));
        ctor.setModifiers(Modifier.Keyword.PUBLIC);
        ctor.setName(clz.getName());

        var pl = properties.entrySet().stream()
                .map(entry -> {
                    var propType = genTypeName(pkg, root, entry.getValue());
                    Parameter parameter = new Parameter(propType, fieldName(entry.getKey()));
                    boolean notNull = isNotNull(schema, entry.getKey());
                    parameter.addAnnotation(mkNullableAnnotation(notNull));
                    annotator.apply(entry.getKey(), entry.getValue()).forEach(parameter::addAnnotation);
                    return parameter;
                }).toList();
        ctor.setParameters(NodeList.nodeList(pl));

        var assignments = properties.entrySet().stream()
                .map(entry -> {
                    var propName = entry.getKey();
                    var propSchema = entry.getValue();
                    String fieldName = fieldName(propName);
                    boolean notNull = isNotNull(schema, entry.getKey());
                    return (Statement) new ExpressionStmt(new AssignExpr(new FieldAccessExpr(
                            new ThisExpr(), fieldName),
                            notNull ? new MethodCallExpr("java.util.Objects.requireNonNull", new NameExpr(fieldName)) : new NameExpr(fieldName),
                            AssignExpr.Operator.ASSIGN));
                })
                .toList();
        ctor.setBody(new BlockStmt(NodeList.nodeList(assignments)));
        return ctor;
    }

    private static boolean isNotNull(SchemaObject objectSchema,
                                     String propertyName) {
        return required(objectSchema, propertyName)
                && objectSchema.getProperties() != null
                && objectSchema.getProperties().containsKey(propertyName)
                && !List.of(SchemaType.NULL).equals(objectSchema.getProperties().get(propertyName).getType());
    }

    private static boolean required(SchemaObject objectSchema, String propertyName) {
        return objectSchema.getRequired() != null
                && objectSchema.getRequired().contains(propertyName);
    }

    @NonNull
    private static TypeModel model(SchemaObject schema) {
        if (schema.getUnknownProperties().get("$$model") != null) {
            return ((TypeModel) schema.getUnknownProperties().get("$$model"));
        }
        else {
            throw new IllegalStateException("Schema lacks a $$model");
        }
    }

    @NonNull
    private static String className(SchemaObject schema) {
        return model(schema).classname();
    }

    private static MethodDeclaration mkToStringMethod(String name, SchemaObject schema) {
        Expression expr = new StringLiteralExpr(name + "[");
        boolean first = true;

        for (String propName : Optional.ofNullable(schema.getProperties()).orElse(Map.of()).keySet()) {
            StringLiteralExpr x;
            if (first) {
                x = new StringLiteralExpr(propName + ": ");
            }
            else {
                x = new StringLiteralExpr(", " + propName + ": ");
            }
            first = false;
            expr = new BinaryExpr(expr, x, BinaryExpr.Operator.PLUS);
            expr = new BinaryExpr(
                    expr,
                    new FieldAccessExpr(new ThisExpr(), fieldName(propName)),
                    BinaryExpr.Operator.PLUS);
        }
        if (additionalPropertiesNotFalse(schema)) {
            expr = new BinaryExpr(
                    expr,
                    new FieldAccessExpr(new ThisExpr(), fieldName(UNKNOWN_PROPERTIES_FIELD_NAME)),
                    BinaryExpr.Operator.PLUS);
        }
        expr = new BinaryExpr(expr, new StringLiteralExpr("]"), BinaryExpr.Operator.PLUS);

        MethodDeclaration methodDeclaration = new MethodDeclaration();
        methodDeclaration.addAnnotation(mkAtOverride());
        methodDeclaration.setModifiers(Modifier.Keyword.PUBLIC);
        methodDeclaration.setType("java.lang.String");
        methodDeclaration.setName("toString");
        methodDeclaration.setBody(new BlockStmt(new NodeList<>(new ReturnStmt(expr))));
        return methodDeclaration;
    }

    private static MethodDeclaration mkHashCodeMethod(SchemaObject schema) {
        NodeList<Expression> args = new NodeList<>();
        for (String propName : Optional.ofNullable(schema.getProperties()).orElse(Map.of()).keySet()) {
            args.add(new FieldAccessExpr(new ThisExpr(), fieldName(propName)));
        }
        if (additionalPropertiesNotFalse(schema)) {
            args.add(new FieldAccessExpr(new ThisExpr(), UNKNOWN_PROPERTIES_FIELD_NAME));
        }

        MethodDeclaration methodDeclaration = new MethodDeclaration();
        methodDeclaration.addAnnotation(mkAtOverride());
        methodDeclaration.setModifiers(Modifier.Keyword.PUBLIC);
        methodDeclaration.setType("int");
        methodDeclaration.setName("hashCode");
        methodDeclaration.setBody(new BlockStmt(new NodeList<>(new ReturnStmt(new MethodCallExpr("java.util.Objects.hash")
                .setArguments(args)))));

        return methodDeclaration;
    }

    private static MethodDeclaration mkEqualsMethod(
                                                    String pkg,
                                                    String className,
                                                    SchemaObject schema) {
        var fieldNames = properties(schema).keySet().stream()
                .map(CodeGen::fieldName)
                .toList();

        String otherParamName = "other";
        String narrowedOtherName = otherParamName + className;
        Expression expr = null;
        if (!fieldNames.isEmpty()) {
            Expression operand = null;
            for (String fieldName : fieldNames) {
                operand = getExpression(fieldName, narrowedOtherName, operand);
            }
            expr = operand;
        }

        if (additionalPropertiesNotFalse(schema)) {
            expr = getExpression(UNKNOWN_PROPERTIES_FIELD_NAME, narrowedOtherName, expr);
        }

        if (expr == null) {
            expr = new BooleanLiteralExpr(true);
        }

        var stmt = new IfStmt(new BinaryExpr(
                new ThisExpr(),
                new NameExpr(otherParamName),
                BinaryExpr.Operator.EQUALS),
                new ReturnStmt(new BooleanLiteralExpr(true)),
                new IfStmt(new InstanceOfExpr(
                        new NameExpr(otherParamName),
                        mkType(className),
                        new TypePatternExpr(new NodeList<>(), mkType(pkg + "." + className), new SimpleName(narrowedOtherName))),
                        new ReturnStmt(expr),
                        new ReturnStmt(new BooleanLiteralExpr(false))));

        MethodDeclaration methodDeclaration = new MethodDeclaration();
        methodDeclaration.addAnnotation(mkAtOverride());
        methodDeclaration.setModifiers(Modifier.Keyword.PUBLIC);
        methodDeclaration.setType("boolean");
        methodDeclaration.setName("equals");
        methodDeclaration.setParameters(new NodeList<>(new Parameter(mkType("java.lang.Object"), otherParamName)));
        methodDeclaration.setBody(new BlockStmt(new NodeList<>(stmt)));
        return methodDeclaration;

    }

    @NonNull
    private static Expression getExpression(String fieldName, String narrowedOtherName, Expression operand) {
        MethodCallExpr call = new MethodCallExpr("java.util.Objects.equals")
                .setArguments(new NodeList<>(
                        new FieldAccessExpr(new ThisExpr(), fieldName),
                        new FieldAccessExpr(new NameExpr(narrowedOtherName), fieldName)));
        if (operand == null) {
            operand = call;
        }
        else {
            operand = new BinaryExpr(
                    operand,
                    call,
                    BinaryExpr.Operator.AND);
        }
        return operand;
    }

    @NonNull
    private static MarkerAnnotationExpr mkAtOverride() {
        return new MarkerAnnotationExpr("java.lang.Override");
    }

    private FieldDeclaration mkPropertyField(SchemaObject object,
                                             String propName,
                                             Type propType) {
        var fieldName = fieldName(propName);
        FieldDeclaration fieldDeclaration = new FieldDeclaration();
        fieldDeclaration.addAnnotation(mkNullableAnnotation(isNotNull(object, propName)));
        VariableDeclarator variable = new VariableDeclarator(propType, fieldName);
        fieldDeclaration.getVariables().add(variable);
        fieldDeclaration.setModifiers(Modifier.Keyword.PRIVATE);
        return fieldDeclaration;
    }

    private FieldDeclaration mkUnknownPropertiesField() {
        FieldDeclaration fieldDeclaration = new FieldDeclaration();
        fieldDeclaration.addAnnotation(mkNullableAnnotation(false));
        VariableDeclarator variable = new VariableDeclarator(mkGenericType("java.util.Map", "java.lang.String", "java.lang.Object"),
                UNKNOWN_PROPERTIES_FIELD_NAME);
        variable.setInitializer(new NullLiteralExpr());
        fieldDeclaration.getVariables().add(variable);
        fieldDeclaration.setModifiers(Modifier.Keyword.PRIVATE);
        return fieldDeclaration;
    }

    // @com.fasterxml.jackson.annotation.JsonSetter(nulls = com.fasterxml.jackson.annotation.Nulls.SKIP)
    @NonNull
    private static NormalAnnotationExpr mkAtJsonSetter() {
        return new NormalAnnotationExpr(new Name("com.fasterxml.jackson.annotation.JsonSetter"),
                NodeList.nodeList(new MemberValuePair("nulls", new FieldAccessExpr(new TypeExpr(
                        mkType("com.fasterxml.jackson.annotation.Nulls")), "SKIP"))));
    }

    @NonNull
    private static MarkerAnnotationExpr mkAtJsonAnySetter() {
        return new MarkerAnnotationExpr(new Name("com.fasterxml.jackson.annotation.JsonAnySetter"));
    }

    @NonNull
    private static MarkerAnnotationExpr mkAtJsonAnyGetter() {
        return new MarkerAnnotationExpr(new Name("com.fasterxml.jackson.annotation.JsonAnyGetter"));
    }

    // @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = )
    private static NormalAnnotationExpr mkAtJsonProperty(String propName, boolean required) {
        NodeList<MemberValuePair> jsonPropertyMembers = NodeList.nodeList(
                new MemberValuePair("value", new StringLiteralExpr(propName)));
        if (required) {
            jsonPropertyMembers.add(new MemberValuePair("required", new BooleanLiteralExpr(true)));
        }
        return new NormalAnnotationExpr(
                new Name("com.fasterxml.jackson.annotation.JsonProperty"),
                jsonPropertyMembers);
    }

    private static NormalAnnotationExpr mkAtJsonDeserialize(Type using) {
        NodeList<MemberValuePair> jsonPropertyMembers = NodeList.nodeList(
                new MemberValuePair("using", new ClassExpr(using)));
        return new NormalAnnotationExpr(
                new Name("com.fasterxml.jackson.databind.annotation.JsonDeserialize"),
                jsonPropertyMembers);
    }

    private static NormalAnnotationExpr mkAtJsonSerialize(Type using) {
        NodeList<MemberValuePair> jsonPropertyMembers = NodeList.nodeList(
                new MemberValuePair("using", new ClassExpr(using)));
        return new NormalAnnotationExpr(
                new Name("com.fasterxml.jackson.databind.annotation.JsonSerialize"),
                jsonPropertyMembers);
    }

    @NonNull
    private AnnotationExpr mkNullableAnnotation(boolean notNull) {
        return new MarkerAnnotationExpr(notNull ? nonNullAnnotation : nullableAnnotation);
    }

    private MethodDeclaration mkPropertyGetterMethod(
                                                     String pkg,
                                                     SchemaObject root,
                                                     SchemaObject object,
                                                     SchemaObject propSchema,
                                                     String propName,
                                                     Type propType) {
        String getterName = propertyStrategy.accessorName(propName);
        String fieldName = fieldName(propName);

        String description = propSchema.getDescription();
        if (description == null) {
            description = "Return the " + propName + ".\n";
        }
        description += "\n@return The value of this object's " + propName + ".\n";

        MethodDeclaration methodDeclaration = new MethodDeclaration();
        methodDeclaration.setJavadocComment(description);
        methodDeclaration.setModifiers(Modifier.Keyword.PUBLIC);
        methodDeclaration.addAnnotation(mkNullableAnnotation(isNotNull(object, propName)));
        methodDeclaration.addAnnotation(mkAtJsonProperty(propName, required(object, propName)));
        if (useMapDeserializer(root, propSchema)) {
            methodDeclaration.addAnnotation(mkAtJsonDeserialize(new ClassOrInterfaceType(genTypeName(pkg, root, object), deserializerClassName(propName))));
            methodDeclaration.addAnnotation(mkAtJsonSerialize(new ClassOrInterfaceType(genTypeName(pkg, root, object), serializerClassName(propName))));
        }
        propertyAnnotators.stream().flatMap(ta -> ta.annotateAccessor(diagnostics, propName, propSchema).stream()).forEach(methodDeclaration::addAnnotation);
        methodDeclaration.setType(propType);
        methodDeclaration.setName(getterName);
        methodDeclaration.setBody(new BlockStmt(new NodeList<>(new ReturnStmt(new FieldAccessExpr(new ThisExpr(), fieldName)))));
        return methodDeclaration;
    }

    private MethodDeclaration mkPropertyOptMethod(SchemaObject root,
                                                  SchemaObject propSchemaOrRef,
                                                  String propName,
                                                  Type propType,
                                                  boolean required) {
        String getterName = propertyStrategy.optionalAccessorName(propName);
        String fieldName = fieldName(propName);

        SchemaObject propSchema = resolveRef(root, propSchemaOrRef);
        String description = propSchema.getDescription();
        if (description == null) {
            description = "Return the " + propName + " as an Optional.\n";
        }
        description += "\n@return The value of this object's " + propName + " as an Optional.\n";

        MethodDeclaration methodDeclaration = new MethodDeclaration();
        methodDeclaration.setJavadocComment(description);
        methodDeclaration.setModifiers(Modifier.Keyword.PUBLIC);
        methodDeclaration.addAnnotation(mkNullableAnnotation(true));
        methodDeclaration.addAnnotation(new MarkerAnnotationExpr("com.fasterxml.jackson.annotation.JsonIgnore"));
        // propertyAnnotators.stream().flatMap(ta -> ta.annotateAccessor(diagnostics, propName, propSchema).stream()).forEach(methodDeclaration::addAnnotation);
        methodDeclaration.setType(mkGenericType("java.util.Optional", propType));
        methodDeclaration.setName(getterName);
        methodDeclaration.setBody(new BlockStmt(new NodeList<>(new ReturnStmt(
                new MethodCallExpr(required ? "java.util.Optional.of" : "java.util.Optional.ofNullable",
                        new FieldAccessExpr(new ThisExpr(), fieldName))))));
        return methodDeclaration;
    }

    @NonNull
    static String fieldName(String propName) {
        return quoteJavaKeyword(quoteNonIdentifierCharacters(propName));
    }

    private MethodDeclaration mkPropertySetterMethod(SchemaObject root,
                                                     SchemaObject object,
                                                     SchemaObject propSchemaOrRef,
                                                     String propName,
                                                     Type propType) {
        var fieldName = fieldName(propName);
        SchemaObject propSchema = resolveRef(root, propSchemaOrRef);
        String description = propSchema.getDescription();
        if (description == null) {
            description = "Set the " + propName + ".\n";
        }
        description += "\n @param " + fieldName + " The new value for this object's " + propName + ".\n";

        MethodDeclaration methodDeclaration = new MethodDeclaration();
        methodDeclaration.setJavadocComment(description);
        propertyAnnotators.stream().flatMap(ta -> ta.annotateMutator(diagnostics, propName, propSchema).stream()).forEach(methodDeclaration::addAnnotation);
        methodDeclaration.setModifiers(Modifier.Keyword.PUBLIC);
        methodDeclaration.setType(new VoidType());
        methodDeclaration.setName(propertyStrategy.mutatorName(propName));
        Parameter parameter = new Parameter(propType, fieldName).addAnnotation(mkNullableAnnotation(isNotNull(object, propName)));
        propertyAnnotators.stream().flatMap(ta -> ta.annotateMutatorParameter(diagnostics, propName, propSchema).stream()).forEach(parameter::addAnnotation);
        methodDeclaration
                .setParameters(new NodeList<>(parameter))
                .setBody(new BlockStmt(new NodeList<>(new ExpressionStmt(new AssignExpr(
                        new FieldAccessExpr(new ThisExpr(), fieldName),
                        isNotNull(object, propName) ? new MethodCallExpr("java.util.Objects.requireNonNull", new NameExpr(fieldName)) : new NameExpr(fieldName),
                        AssignExpr.Operator.ASSIGN)))));

        return methodDeclaration;
    }

    @NonNull
    static String quoteNonIdentifierCharacters(String memberName) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < memberName.length(); i++) {
            int codePoint = memberName.codePointAt(i);
            if (i == 0 ? Character.isJavaIdentifierStart(codePoint)
                    : Character.isJavaIdentifierPart(codePoint)) {
                builder.appendCodePoint(codePoint);
            }
            else {
                builder.append("_");
            }
        }
        return builder.toString();
    }

    @NonNull
    static String quoteJavaKeyword(String ident) {
        return switch (ident) {
            // TODO check we got them all
            case "null", "boolean", "int", "byte", "short", "long", "float", "double", "char", "void" -> ident + "_";
            case "public", "private", "protected" -> ident + "_";
            case "class", "interface", "enum", "package", "module", "implements", "extends" -> ident + "_";
            case "static", "abstract", "final", "transient", "super", "this" -> ident + "_";
            case "return", "break", "continue", "for", "while", "switch", "case", "default", "if", "else", "goto" -> ident + "_";
            default -> ident;
        };
    }

    record Unit(
                URI schemaUri,
                CompilationUnit compilationUnit,
                TypeModel typeModel) {

    }

    private class CodeGenVisitor extends SchemaVisitor {
        private final SchemaInput input;
        private final List<Unit> units;

        CodeGenVisitor(
                       SchemaInput input,
                       List<Unit> units) {
            this.input = input;
            this.units = units;
        }

        @Override
        public void enterSchema(SchemaVisitor.Context context, SchemaObject schema) {
            if (schema.getRef() == null) {
                if (isJunctorChild(context.keyword())) {
                    return;
                }
                if (schema.getType() == null
                        && schema.getProperties() == null
                        && schema.getItems() == null
                        && schema.getAdditionalProperties() == null
                        && schema.getPatternProperties() == null
                        && schema.getRequired() == null) {
                    // It's OK to have a schema just for its definitions, for example
                    return;
                }
                // We don't generate code for a ref, on the basis that we've already generated code for it
                // (e.g. when we visited the schemas in /definitions).
                // This means even if multiple refs point to the same thing, that thing should only get code gen'd once.
                Unit unit = genDecl(input.pkg(), schema, context.fullPath(), context.base());
                if (unit != null) {
                    units.add(unit);
                }
            }
        }

        /**
         * Is the given path a child of {@code allOf}, {@code oneOf}, {@code anyOf} or {@code not}
         * @param keyword The keyword
         * @return true the schema at this path is a child of a logical junctor
         */
        private boolean isJunctorChild(String keyword) {
            return "oneOf".equals(keyword)
                    || "allOf".equals(keyword)
                    || "anyOf".equals(keyword)
                    || "not".equals(keyword);

        }
    }

}
