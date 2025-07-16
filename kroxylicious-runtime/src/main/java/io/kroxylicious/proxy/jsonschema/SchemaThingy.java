/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.jsonschema;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.kroxylicious.proxy.config.Configuration;
import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;
import io.kroxylicious.proxy.plugin.PluginImplConfig;
import io.kroxylicious.proxy.plugin.PluginImplName;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class SchemaThingy {

//    private static final ObjectMapper MAPPER = new YAMLMapper()
//            .disable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
//            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final ObjectNode rootSchemaObject;
    private final ObjectNode defs;


    public SchemaThingy() {
        this.rootSchemaObject = MAPPER.createObjectNode();
        //rootSchemaObject.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        rootSchemaObject.put("$schema", "https://json-schema.org/draft-07/schema");
        rootSchemaObject.put("$id", "https://kroxylicious.io/schema/0.14.0/configuration.yaml");
        this.defs = rootSchemaObject.putObject("$defs");
    }

    String schemaFor(String path, Class<?> cls) {
        String defName = defName(cls);
        if (!defs.has(defName)) {
            ObjectNode value = schemaObject(path, cls);
            defs.set(defName, value);
        }
        return "#/$defs/" + defName;
    }

    @NonNull
    private static String defName(Class<?> cls) {
        String defName;
        if (Integer.class.equals(cls)) {
            defName = Integer.TYPE.getName();
        }
        else {
            defName = cls.getName();
        }
        return defName;
    }

    @NonNull
    private static String defUrl(Class<?> type) {
        return "#/$defs/" + defName(type);
    }

    private static final ServiceBasedPluginFactoryRegistry pluginFactoryRegistry = new ServiceBasedPluginFactoryRegistry();

    public static void main(String[] a) throws IOException {
        SchemaThingy schemaThingy = new SchemaThingy();
        var def = schemaThingy.schemaFor("$", Configuration.class);
        schemaThingy.rootSchemaObject.put("$ref", def);
        schemaThingy.defs.putObject("$schema").put("type", "string");
        MAPPER.writeValue(System.out, schemaThingy.rootSchemaObject);
    }

    private ObjectNode schemaObject(String path, Class<?> cls) {
        Objects.requireNonNull(cls);
        if (String.class.equals(cls)) {
            return MAPPER.createObjectNode().put("type", "string");
        }
        else if (Boolean.class.equals(cls)
                || Boolean.TYPE.equals(cls)) {
            return MAPPER.createObjectNode().put("type", "boolean");
        }
        else if (Integer.class.equals(cls)
                || Integer.TYPE.equals(cls)) {
            return MAPPER.createObjectNode()
                    .put("type", "integer")
                    .put("minimum", Integer.MIN_VALUE)
                    .put("maximum", Integer.MAX_VALUE);

        }
        else if (Long.class.equals(cls)
                || Long.TYPE.equals(cls)) {
            return MAPPER.createObjectNode().put("type", "integer")
                    .put("minimum", Long.MIN_VALUE)
                    .put("maximum", Long.MAX_VALUE);
        }
        else if (Short.class.equals(cls)
                || Short.TYPE.equals(cls)) {
            return MAPPER.createObjectNode().put("type", "integer")
                    .put("minimum", Short.MIN_VALUE)
                    .put("maximum", Short.MAX_VALUE);
        }
        else if (Double.class.equals(cls)
                || Double.TYPE.equals(cls)
                || Float.class.equals(cls)
                || Float.TYPE.equals(cls)) {
            return MAPPER.createObjectNode().put("type", "number");
        }
        else if (cls.isRecord()) {
            List<Property> props = null;
            for (var ctor : cls.getDeclaredConstructors()) {
                if (ctor.isAnnotationPresent(JsonCreator.class)) {
                    props = propertiesForConstructor(ctor);
                    break;
                }
            }
            if (props == null) {
                props = propertiesForRecord(cls.getRecordComponents());
            }
            return objectTypedSchemaObject(path, props);
        }
        else if (cls.isEnum()) {
            var schemaObject = MAPPER.createObjectNode();
            schemaObject.put("type", "string");
            var enumArray = schemaObject.putArray("enum");
            for (var symbol : cls.getEnumConstants()) {
                enumArray.add(symbol.toString());
            }
            return schemaObject;
        }
        else if (cls.isInterface()) {
            var subtypes = cls.getAnnotation(JsonSubTypes.class);
            if (subtypes != null) {
                var schemaObject = MAPPER.createObjectNode();
                var union = schemaObject.putArray("oneOf");
                for (var jacksonType : subtypes.value()) {
                    schemaFor("", jacksonType.value());
                    union.add(MAPPER.createObjectNode().put("$ref", defUrl(jacksonType.value())));
                }
                return schemaObject;
            }
            else {
                throw new UnsupportedOperationException("Cannot figure out the possible subclasses of interface " + cls.getName() + " at " + path);
            }
        }
        else {
            List<Property> props = null;
            for (var ctor : cls.getDeclaredConstructors()) {
                if (ctor.isAnnotationPresent(JsonCreator.class)) {
                    return objectTypedSchemaObject(path, propertiesForConstructor(ctor));
                }
            }
            for (var meth : cls.getMethods()) {
                if (meth.isAnnotationPresent(JsonCreator.class)) {
                    if (meth.getParameterCount() == 1) {
                        Parameter parameter = meth.getParameters()[0];
                        return schemaObject(path + "/" + parameter.getName(), parameter.getType());
                    }
                    else {
                        throw new UnsupportedOperationException("Factory method " + meth + " at " + path);
                    }
                }
            }
//            if (props == null) {
//                props = properties(cls.getRecordComponents());
//            }
//            return objectTypedSchemaObject(path, props);

            throw new UnsupportedOperationException(cls + " at " + path);
            //return MAPPER.createObjectNode();
        }
    }

    @NonNull
    private ObjectNode objectTypedSchemaObject(String path, List<Property> props) {
        var schemaObject = MAPPER.createObjectNode();
        schemaObject.put("type", "object");
        schemaObject.put("additionalProperties", false);
        var properties = schemaObject.putObject("properties");

        for (var recordComponent : props) {
            var propertyAnno = recordComponent.getAnnotation(JsonProperty.class);
            String propertyName;
            if (propertyAnno != null
                    && propertyAnno.value() != null
                    && !propertyAnno.value().equals(JsonProperty.USE_DEFAULT_NAME)) {
                propertyName = propertyAnno.value();
            }
            else {
                propertyName = recordComponent.getName();
            }

            properties.set(propertyName, propertySchema(path, recordComponent, propertyName));

            JsonAlias aliasAnno = recordComponent.getAnnotation(JsonAlias.class);
            if (aliasAnno != null) {
                for (var alias : aliasAnno.value()) {
                    properties.set(alias, propertySchema(path, recordComponent, propertyName));
                }
            }
        }
        return schemaObject;
    }

    @Nullable
    private ObjectNode propertySchema(String path,
                                      Property property,
                                      String propertyName) {
        final ObjectNode propertySchema;

        if (property.getAnnotation(PluginImplName.class) != null) {
            ObjectNode put = MAPPER.createObjectNode().put("type", "string");
            var cases = put.putArray("enum");
            pluginFactoryRegistry.pluginImplementations(property.getAnnotation(PluginImplName.class).value()).forEach(cases::add);
            return put;
        }
        else if (property.getAnnotation(PluginImplConfig.class) != null) {
//            property.getAnnotation(PluginImplConfig.class).implNameProperty();
//            pluginFactoryRegistry.pluginFactory(null).configType(configType)// TODO recurse into this type??
            return MAPPER.createObjectNode().put("type", "object");
        }

        Class<?> propertyType = property.getType();
        if (String.class.equals(propertyType)) {
            propertySchema = MAPPER.createObjectNode().put("type", "string");
        }
        else if (Boolean.class.equals(propertyType)
                || Boolean.TYPE.equals(propertyType)) {
            propertySchema = MAPPER.createObjectNode().put("type", "boolean");
        }
        else if (Optional.class.equals(propertyType)) {
            if (property.getGenericType() instanceof ParameterizedType pt) {
                if (pt.getActualTypeArguments()[0] instanceof Class<?> itemClass) {
                    propertySchema = schemaObject(path + "/" + propertyName, itemClass);
                    schemaFor(path + "/" + propertyName, itemClass);
                }
                else {
                    propertySchema = MAPPER.createObjectNode();
                    propertySchema.put("type", "object");
                }
            }
            else {
                propertySchema = MAPPER.createObjectNode();
                propertySchema.put("type", "object");
            }
        }
        else if (Map.class.equals(propertyType)) {
            // TODO check the key type is String
            propertySchema = MAPPER.createObjectNode();
            propertySchema.put("type", "object");
        }
        else if (List.class.equals(propertyType)
                || Set.class.equals(propertyType)) {
            propertySchema = MAPPER.createObjectNode();
            propertySchema.put("type", "array");
            Type genericType = property.getGenericType();
            if (genericType instanceof ParameterizedType pt) {
                Type actualTypeArgument = pt.getActualTypeArguments()[0];
                if (actualTypeArgument instanceof Class<?> itemClass) {
                    propertySchema.putObject("items")
                            .put("$ref", schemaFor(path + "/" + propertyName, itemClass));
                }
                else {
                    // TODO propertySchema.put("items", "bob");
                }
            }
            else {
                // TODO propertySchema.put("items", "bob");
            }
        }
        // optional, duration, long, int, double, float, set?
        else {

            if (property.getAnnotation(PluginImplConfig.class) != null) {
                propertySchema = MAPPER.createObjectNode().put("type", "object");
            }
            else {
                Class<?> type = propertyType;
                schemaFor(path + "/" + propertyName, type);
                propertySchema = MAPPER.createObjectNode().put("$ref", defUrl(type));
            }
        }
        return propertySchema;
    }

    private List<Property> propertiesForRecord(RecordComponent[] recordComponents) {
        return Arrays.stream(recordComponents).map(RecordComponentProperty::new).collect(Collectors.toList());
    }

    private List<Property> propertiesForConstructor(Constructor<?> constructor) {
        return IntStream.range(0, constructor.getParameterCount()).mapToObj(index -> new RecordParameterProperty(constructor, index)).collect(Collectors.toList());
    }

    private List<Property> propertiesForFactory(Executable method) {
        return IntStream.range(0, 1).mapToObj(index -> new RecordParameterProperty(method, index)).collect(Collectors.toList());
    }



    interface Property {

        Class<?> getType();

        <T extends Annotation> @Nullable T getAnnotation(Class<T> pluginImplConfigClass);

        Type getGenericType();

        String getName();
    }

    record RecordComponentProperty(RecordComponent recordComponent) implements Property {

        @Override
        public Class<?> getType() {
            return recordComponent.getType();
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return recordComponent.getAnnotation(annotationClass);
        }

        @Override
        public Type getGenericType() {
            return recordComponent.getGenericType();
        }

        @Override
        public String getName() {
            return recordComponent.getName();
        }
    }

    record RecordParameterProperty(Executable constructor, int index) implements Property {

        @Override
        public Class<?> getType() {
            return constructor.getParameterTypes()[index];
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return constructor.getParameters()[index].getAnnotation(annotationClass);
        }

        @Override
        public Type getGenericType() {
            return constructor.getGenericParameterTypes()[index];
        }

        @Override
        public String getName() {
            return constructor.getParameters()[index].getName();
        }
    }
}
