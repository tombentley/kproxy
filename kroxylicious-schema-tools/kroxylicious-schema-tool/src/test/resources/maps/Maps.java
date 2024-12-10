/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package maps;

/**
 * An class with properties mapped from the array type.
 */
@javax.annotation.processing.Generated("io.kroxylicious.tools.schema.compiler.CodeGen")
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "keyedOnFoo", "objectAsMap", "mixed" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class Maps {

    static class KeyedOnFooDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<java.util.Map<java.lang.String, maps.FooBarBaz>> {

        @java.lang.Override
        public java.util.Map<java.lang.String, maps.FooBarBaz> deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.ObjectMapper mapper = (com.fasterxml.jackson.databind.ObjectMapper) parser.getCodec();
            java.util.List<maps.FooBarBaz> list = mapper.readValue(parser, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<maps.FooBarBaz>>() {
            });
            java.util.Map<java.lang.String, maps.FooBarBaz> result = new java.util.LinkedHashMap<>();
            for (var item : list) result.put(item.foo(), item);
            return result;
        }
    }

    static class KeyedOnFooSerializer extends com.fasterxml.jackson.databind.JsonSerializer<java.util.Map<java.lang.String, maps.FooBarBaz>> {

        @java.lang.Override
        public void serialize(java.util.Map<java.lang.String, maps.FooBarBaz> map, com.fasterxml.jackson.core.JsonGenerator generator, com.fasterxml.jackson.databind.SerializerProvider provider) throws java.io.IOException {
            generator.writeStartArray();
            for (var item : map.values()) generator.writeObject(item);
            generator.writeEndArray();
        }
    }

    @edu.umd.cs.findbugs.annotations.Nullable
    private java.util.Map<java.lang.String, maps.FooBarBaz> keyedOnFoo;

    @edu.umd.cs.findbugs.annotations.Nullable
    private java.util.Map<java.lang.String, maps.FooBarBaz> objectAsMap;

    @edu.umd.cs.findbugs.annotations.Nullable
    private maps.MapsMixed mixed;

    /**
     * All properties constructor.
     * @param keyedOnFoo The value of the {@code keyedOnFoo} property. This is an optional property.
     * @param objectAsMap The value of the {@code objectAsMap} property. This is an optional property.
     * @param mixed The value of the {@code mixed} property. This is an optional property.
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public Maps(@edu.umd.cs.findbugs.annotations.Nullable @com.fasterxml.jackson.annotation.JsonProperty(value = "keyedOnFoo") java.util.Map<java.lang.String, maps.FooBarBaz> keyedOnFoo, @edu.umd.cs.findbugs.annotations.Nullable @com.fasterxml.jackson.annotation.JsonProperty(value = "objectAsMap") java.util.Map<java.lang.String, maps.FooBarBaz> objectAsMap, @edu.umd.cs.findbugs.annotations.Nullable @com.fasterxml.jackson.annotation.JsonProperty(value = "mixed") maps.MapsMixed mixed) {
        this.keyedOnFoo = keyedOnFoo;
        this.objectAsMap = objectAsMap;
        this.mixed = mixed;
    }

    /**
     * An array of FooBars
     * @return The value of this object's keyedOnFoo.
     */
    @edu.umd.cs.findbugs.annotations.Nullable
    @com.fasterxml.jackson.annotation.JsonProperty(value = "keyedOnFoo")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = maps.Maps.KeyedOnFooDeserializer.class)
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = maps.Maps.KeyedOnFooSerializer.class)
    public java.util.Map<java.lang.String, maps.FooBarBaz> keyedOnFoo() {
        return this.keyedOnFoo;
    }

    /**
     * An array of FooBars
     *  @param keyedOnFoo The new value for this object's keyedOnFoo.
     */
    public void keyedOnFoo(@edu.umd.cs.findbugs.annotations.Nullable java.util.Map<java.lang.String, maps.FooBarBaz> keyedOnFoo) {
        this.keyedOnFoo = keyedOnFoo;
    }

    /**
     * Return the objectAsMap.
     *
     * @return The value of this object's objectAsMap.
     */
    @edu.umd.cs.findbugs.annotations.Nullable
    @com.fasterxml.jackson.annotation.JsonProperty(value = "objectAsMap")
    public java.util.Map<java.lang.String, maps.FooBarBaz> objectAsMap() {
        return this.objectAsMap;
    }

    /**
     * Set the objectAsMap.
     *
     *  @param objectAsMap The new value for this object's objectAsMap.
     */
    public void objectAsMap(@edu.umd.cs.findbugs.annotations.Nullable java.util.Map<java.lang.String, maps.FooBarBaz> objectAsMap) {
        this.objectAsMap = objectAsMap;
    }

    /**
     * Return the mixed.
     *
     * @return The value of this object's mixed.
     */
    @edu.umd.cs.findbugs.annotations.Nullable
    @com.fasterxml.jackson.annotation.JsonProperty(value = "mixed")
    public maps.MapsMixed mixed() {
        return this.mixed;
    }

    /**
     * Set the mixed.
     *
     *  @param mixed The new value for this object's mixed.
     */
    public void mixed(@edu.umd.cs.findbugs.annotations.Nullable maps.MapsMixed mixed) {
        this.mixed = mixed;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Maps[" + "keyedOnFoo: " + this.keyedOnFoo + ", objectAsMap: " + this.objectAsMap + ", mixed: " + this.mixed + "]";
    }

    @java.lang.Override
    public int hashCode() {
        return java.util.Objects.hash(this.keyedOnFoo, this.objectAsMap, this.mixed);
    }

    @java.lang.Override
    public boolean equals(java.lang.Object other) {
        if (this == other)
            return true;
        else if (other instanceof maps.Maps otherMaps)
            return java.util.Objects.equals(this.keyedOnFoo, otherMaps.keyedOnFoo) && java.util.Objects.equals(this.objectAsMap, otherMaps.objectAsMap) && java.util.Objects.equals(this.mixed, otherMaps.mixed);
        else
            return false;
    }
}