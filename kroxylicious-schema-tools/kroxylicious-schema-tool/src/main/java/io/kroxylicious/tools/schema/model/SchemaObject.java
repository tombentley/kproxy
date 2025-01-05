/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.model;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.sundr.builder.annotations.Buildable;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false)
public final class SchemaObject {
    @Nullable
    @JsonProperty("$schema")
    private String schema;

    @Nullable
    @JsonProperty("$ref")
    private String ref;

    @Nullable
    @JsonProperty("id")
    private String id;

    @Nullable
    @JsonProperty("multipleOf")
    private Double multipleOf;

    @Nullable
    @JsonProperty("maximum")
    private Double maximum;

    @Nullable
    @JsonProperty("exclusiveMaximum")
    private Double exclusiveMaximum;

    @Nullable
    @JsonProperty("minimum")
    private Double minimum;

    @Nullable
    @JsonProperty("exclusiveMinimum")
    private Double exclusiveMinimum;

    @Nullable
    @JsonProperty("maxLength")
    private Long maxLength;

    @Nullable
    @JsonProperty("minLength")
    private Long minLength;

    @Nullable
    @JsonProperty("pattern")
    private String pattern;

    @Nullable
    @JsonProperty("items")
    @JsonDeserialize(using = ListOrSingleSerde.SchemaObject.class)
    private List<SchemaObject> items;

    @Nullable
    @JsonProperty("additionalItems")
    @JsonDeserialize(using = ListOrSingleSerde.SchemaObject.class)
    private SchemaValue additionalItems;

    @Nullable
    @JsonProperty("maxItems")
    private Long maxItems;

    @Nullable
    @JsonProperty("minItems")
    private Long minItems;

    @Nullable
    @JsonProperty("uniqueItems")
    private Boolean uniqueItems;

    @Nullable
    @JsonProperty("maxProperties")
    private Long maxProperties;

    @Nullable
    @JsonProperty("minProperties")
    private Long minProperties;

    @Nullable
    @JsonProperty("required")
    private Set<String> required;

    @Nullable
    @JsonProperty("properties")
    private Map<String, SchemaObject> properties;

    @Nullable
    @JsonProperty("patternProperties")
    private Map<String, SchemaObject> patternProperties;

    @Nullable
    @JsonProperty("additionalProperties")
    private SchemaValue additionalProperties;

    // TODO dependencies
    @Nullable
    @JsonProperty("enum")
    private List<Object> enum_;

    @Nullable
    @JsonProperty("type")
    @JsonDeserialize(using = ListOrSingleSerde.SchemaType.class)
    private List<SchemaType> type;

    @Nullable
    @JsonProperty("allOf")
    @JsonDeserialize(using = ListOrSingleSerde.SchemaObject.class)
    private List<SchemaObject> allOf;

    @Nullable
    @JsonProperty("anyOf")
    @JsonDeserialize(using = ListOrSingleSerde.SchemaObject.class)
    private List<SchemaObject> anyOf;

    @Nullable
    @JsonProperty("oneOf")
    @JsonDeserialize(using = ListOrSingleSerde.SchemaObject.class)
    private List<SchemaObject> oneOf;

    @Nullable
    @JsonProperty("not")
    private SchemaObject not;

    @Nullable
    @JsonProperty("definitions")
    private Map<String, SchemaObject> definitions;

    @Nullable
    @JsonProperty("title")
    private String title;

    @Nullable
    @JsonProperty("description")
    private String description;

    @Nullable
    @JsonProperty("default")
    private Object default_;

    @Nullable
    @JsonProperty("format")
    private String format;

    // TODO nullable
    // TODO descriminator
    // TODO readOnly
    // TODO writeOnly
    // TODO xml
    // externalDocs
    // example

    // TODO x-kubernetes-validation

    @Nullable
    @JsonProperty("x-kubernetes-list-type")
    private XKubeListType xKubernetesListType;

    @Nullable
    @JsonProperty("x-kubernetes-list-map-keys")
    private List<String> xKubernetesListMapKeys;

    @Nullable
    @JsonProperty("x-kubernetes-map-type")
    private XKubeMapType xKubernetesMapType;

    // TODO x-kubernetes-int-or-string
    // TODO x-kubernetes-preserve-unknown-fields
    // TODO x-kubernetes-embedded-resource

    @Nullable
    @JsonProperty("$javaType")
    private String javaType;

    @JsonCreator
    public SchemaObject() {
        super();
    }

    @Nullable
    public String getSchema() {
        return schema;
    }

    public void setSchema(@Nullable String schema) {
        this.schema = schema;
    }

    @Nullable
    public String getRef() {
        return ref;
    }

    public void setRef(@Nullable String ref) {
        this.ref = ref;
    }

    @Nullable
    public String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    @Nullable
    public Double getMultipleOf() {
        return multipleOf;
    }

    public void setMultipleOf(@Nullable Double multipleOf) {
        this.multipleOf = multipleOf;
    }

    @Nullable
    public Double getMaximum() {
        return maximum;
    }

    public void setMaximum(@Nullable Double maximum) {
        this.maximum = maximum;
    }

    @Nullable
    public Double getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public void setExclusiveMaximum(@Nullable Double exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    @Nullable
    public Double getMinimum() {
        return minimum;
    }

    public void setMinimum(@Nullable Double minimum) {
        this.minimum = minimum;
    }

    @Nullable
    public Double getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public void setExclusiveMinimum(@Nullable Double exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    @Nullable
    public Long getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(@Nullable Long maxLength) {
        this.maxLength = maxLength;
    }

    @Nullable
    public Long getMinLength() {
        return minLength;
    }

    public void setMinLength(@Nullable Long minLength) {
        this.minLength = minLength;
    }

    @Nullable
    public String getPattern() {
        return pattern;
    }

    public void setPattern(@Nullable String pattern) {
        this.pattern = pattern;
    }

    @Nullable
    public List<SchemaObject> getItems() {
        return items;
    }

    public void setItems(@Nullable List<SchemaObject> items) {
        this.items = items;
    }

    @Nullable
    public SchemaValue getAdditionalItems() {
        return additionalItems;
    }

    public void setAdditionalItems(@Nullable SchemaValue additionalItems) {
        this.additionalItems = additionalItems;
    }

    @Nullable
    public Long getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(@Nullable Long maxItems) {
        this.maxItems = maxItems;
    }

    @Nullable
    public Long getMinItems() {
        return minItems;
    }

    public void setMinItems(@Nullable Long minItems) {
        this.minItems = minItems;
    }

    @Nullable
    public Boolean getUniqueItems() {
        return uniqueItems;
    }

    public void setUniqueItems(@Nullable Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
    }

    @Nullable
    public Long getMaxProperties() {
        return maxProperties;
    }

    public void setMaxProperties(@Nullable Long maxProperties) {
        this.maxProperties = maxProperties;
    }

    @Nullable
    public Long getMinProperties() {
        return minProperties;
    }

    public void setMinProperties(@Nullable Long minProperties) {
        this.minProperties = minProperties;
    }

    @Nullable
    public Set<String> getRequired() {
        return required;
    }

    public void setRequired(@Nullable Set<String> required) {
        this.required = required;
    }

    @Nullable
    public Map<String, SchemaObject> getProperties() {
        return properties;
    }

    public void setProperties(@Nullable Map<String, SchemaObject> properties) {
        this.properties = properties;
    }

    @Nullable
    public Map<String, SchemaObject> getPatternProperties() {
        return patternProperties;
    }

    public void setPatternProperties(@Nullable Map<String, SchemaObject> patternProperties) {
        this.patternProperties = patternProperties;
    }

    @Nullable
    public SchemaValue getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(@Nullable SchemaValue additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @Nullable
    public List<SchemaType> getType() {
        return type;
    }

    public void setType(@Nullable List<SchemaType> type) {
        this.type = type;
    }

    @Nullable
    public List<Object> getEnum() {
        return enum_;
    }

    public void setEnum(@Nullable List<Object> enum_) {
        this.enum_ = enum_;
    }

    @Nullable
    public List<SchemaObject> getAllOf() {
        return allOf;
    }

    public void setAllOf(@Nullable List<SchemaObject> allOf) {
        this.allOf = allOf;
    }

    @Nullable
    public List<SchemaObject> getAnyOf() {
        return anyOf;
    }

    public void setAnyOf(@Nullable List<SchemaObject> anyOf) {
        this.anyOf = anyOf;
    }

    @Nullable
    public List<SchemaObject> getOneOf() {
        return oneOf;
    }

    public void setOneOf(@Nullable List<SchemaObject> oneOf) {
        this.oneOf = oneOf;
    }

    @Nullable
    public SchemaObject getNot() {
        return not;
    }

    public void setNot(@Nullable SchemaObject not) {
        this.not = not;
    }

    @Nullable
    public Map<String, SchemaObject> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(@Nullable Map<String, SchemaObject> definitions) {
        this.definitions = definitions;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public Object getDefault() {
        return default_;
    }

    public void setDefault(@Nullable Object default_) {
        this.default_ = default_;
    }

    @Nullable
    public String getFormat() {
        return format;
    }

    public void setFormat(@Nullable String format) {
        this.format = format;
    }

    @Nullable
    public XKubeListType getXKubernetesListType() {
        return xKubernetesListType;
    }

    @Nullable
    public List<String> getXKubernetesListMapKeys() {
        return xKubernetesListMapKeys;
    }

    @Nullable
    public XKubeMapType getXKubernetesMapType() {
        return xKubernetesMapType;
    }

    @Nullable
    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(@Nullable String javaType) {
        this.javaType = javaType;
    }

    public void setXKubernetesListMapKeys(@Nullable List<String> xKubernetesListMapKeys) {
        this.xKubernetesListMapKeys = xKubernetesListMapKeys;
    }

    public void setXKubernetesListType(@Nullable XKubeListType xKubernetesListType) {
        this.xKubernetesListType = xKubernetesListType;
    }

    public void setXKubernetesMapType(@Nullable XKubeMapType xKubernetesMapType) {
        this.xKubernetesMapType = xKubernetesMapType;
    }

    private Map<String, Object> unknownProperties;

    @JsonAnyGetter
    public @NonNull Map<String, Object> getUnknownProperties() {
        return this.unknownProperties != null ? this.unknownProperties : Map.of();
    }

    @JsonAnySetter
    public void setUnknownProperty(String name, Object value) {
        if (this.unknownProperties == null) {
            this.unknownProperties = new HashMap<>(2);
        }
        this.unknownProperties.put(name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SchemaObject that)) {
            return false;
        }
        return Objects.equals(schema, that.schema) && Objects.equals(id, that.id) && Objects.equals(ref, that.ref) && Objects.equals(description, that.description)
                && Objects.equals(definitions, that.definitions) && Objects.equals(type, that.type) && Objects.equals(format, that.format) && Objects.equals(properties,
                        that.properties)
                && Objects.equals(required, that.required) && Objects.equals(items, that.items) && Objects.equals(oneOf, that.oneOf) && Objects.equals(
                        allOf, that.allOf)
                && Objects.equals(anyOf, that.anyOf) && Objects.equals(not, that.not) && xKubernetesListType == that.xKubernetesListType
                && Objects.equals(xKubernetesListMapKeys, that.xKubernetesListMapKeys) && xKubernetesMapType == that.xKubernetesMapType && Objects.equals(javaType,
                        that.javaType)
                && Objects.equals(additionalProperties, that.additionalProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, id, ref, description, definitions, type, format, properties, required, items, oneOf, allOf, anyOf, not, xKubernetesListType,
                xKubernetesListMapKeys, xKubernetesMapType, javaType, additionalProperties);
    }

    @Override
    public String toString() {
        return "SchemaObject{" +
                "schema='" + schema + '\'' +
                ", id='" + id + '\'' +
                ", ref='" + ref + '\'' +
                ", description='" + description + '\'' +
                ", definitions=" + definitions +
                ", type=" + type +
                ", format='" + format + '\'' +
                ", properties=" + properties +
                ", required=" + required +
                ", items=" + items +
                ", oneOf=" + oneOf +
                ", allOf=" + allOf +
                ", anyOf=" + anyOf +
                ", not=" + not +
                ", xKubernetesListType=" + xKubernetesListType +
                ", xKubernetesListMapKeys=" + xKubernetesListMapKeys +
                ", xKubernetesMapType=" + xKubernetesMapType +
                ", javaType='" + javaType + '\'' +
                ", additionalProperties=" + additionalProperties +
                '}';
    }

    public void visitSchemas(Reporting diagnostics, URI base, @NonNull SchemaVisitor visitor) throws VisitorException {
        var context = new SchemaVisitor.Context(diagnostics, base);
        visitSchemas(context, this, visitor);
    }

    private static void visitSchemas(SchemaVisitor.Context context,
                                     SchemaObject schemaObject,
                                     @NonNull SchemaVisitor visitor)
            throws VisitorException {

        SchemaVisitor.VisitAction action;
        try {
            action = visitor.enterSchema(context, schemaObject);
        }
        catch (Exception e) {
            throw new VisitorException(
                    visitor.getClass().getName() + "#enterSchema() threw exception while visiting schema object at '" + context.fullPath() + "' from " + context.base(),
                    e);
        }
        if (action == SchemaVisitor.VisitAction.CONTINUE) {
            visitSchemaMap(context, visitor, schemaObject, schemaObject.definitions, "definitions");
            visitSchemaMap(context, visitor, schemaObject, schemaObject.properties, "properties");
            visitSchemaArray(context, visitor, schemaObject, schemaObject.items, "items");
            visitSchemaArray(context, visitor, schemaObject, schemaObject.oneOf, "oneOf");
            visitSchemaArray(context, visitor, schemaObject, schemaObject.allOf, "allOf");
            visitSchemaArray(context, visitor, schemaObject, schemaObject.anyOf, "anyOf");
            if (schemaObject.not != null) {
                String path1 = "not";
                visitSchemas(context.sub("not", path1, schemaObject), schemaObject.not, visitor);
            }
        }
        try {
            visitor.exitSchema(context, schemaObject);
        }
        catch (Exception e) {
            throw new VisitorException(
                    visitor.getClass().getName() + "#exitSchema() threw exception while visiting schema object at '" + context.fullPath() + "' from " + context.base(),
                    e);
        }
    }

    private static void visitSchemaMap(SchemaVisitor.Context context,
                                       SchemaVisitor visitor,
                                       SchemaObject parent,
                                       @Nullable Map<String, SchemaObject> map,
                                       String keyword) {
        if (map != null) {
            for (Map.Entry<String, SchemaObject> entry : map.entrySet()) {
                String definitionName = entry.getKey();
                SchemaObject definitionSchema = entry.getValue();
                String path1 = keyword + "/" + definitionName;
                visitSchemas(context.sub(keyword, path1, parent), definitionSchema, visitor);
            }
        }
    }

    private static void visitSchemaArray(SchemaVisitor.Context context,
                                         SchemaVisitor visitor,
                                         SchemaObject parent,
                                         @Nullable List<SchemaObject> array,
                                         String keyword) {
        if (array != null) {
            for (int i = 0; i < array.size(); i++) {
                SchemaObject itemSchema = array.get(i);
                String path1 = keyword + "/" + i;
                visitSchemas(context.sub(keyword, path1, parent), itemSchema, visitor);
            }
        }
    }
}
