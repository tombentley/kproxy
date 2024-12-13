/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package rootisref;

/**
 * There's a sibling schema that $refs to me
 */
@javax.annotation.processing.Generated("io.kroxylicious.tools.schema.compiler.CodeGen")
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "name" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class Other {

    @edu.umd.cs.findbugs.annotations.Nullable
    private java.lang.String name;

    @edu.umd.cs.findbugs.annotations.Nullable
    private java.util.Map<java.lang.String, java.lang.Object> unknownProperties = null;

    /**
     * All properties constructor.
     * @param name The value of the {@code name} property. This is an optional property.
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public Other(@edu.umd.cs.findbugs.annotations.Nullable @com.fasterxml.jackson.annotation.JsonProperty(value = "name") java.lang.String name) {
        this.name = name;
    }

    /**
     * Return the name.
     *
     * @return The value of this object's name.
     */
    @edu.umd.cs.findbugs.annotations.Nullable
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name")
    public java.lang.String name() {
        return this.name;
    }

    /**
     * Set the name.
     *
     *  @param name The new value for this object's name.
     */
    public void name(@edu.umd.cs.findbugs.annotations.Nullable java.lang.String name) {
        this.name = name;
    }

    /**
     * Get any additional properties not declared in the schema.
     * @return value The properties.
     */
    @edu.umd.cs.findbugs.annotations.NonNull
    @com.fasterxml.jackson.annotation.JsonAnyGetter
    public java.util.Map<java.lang.String, java.lang.Object> getAdditionalProperties() {
        return this.unknownProperties == null ? java.util.Map.of() : this.unknownProperties;
    }

    /**
     * Add an additional property not declared in the schema.
     * @param name The name of the property.
     * @param value The value of the property.
     */
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void setAdditionalProperty(@edu.umd.cs.findbugs.annotations.NonNull java.lang.String name, @edu.umd.cs.findbugs.annotations.NonNull java.lang.Object value) {
        java.util.Objects.requireNonNull(name);
        if (this.unknownProperties == null)
            this.unknownProperties = new java.util.HashMap<>();
        this.unknownProperties.put(name, value);
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Other[" + "name: " + this.name + this.unknownProperties + "]";
    }

    @java.lang.Override
    public int hashCode() {
        return java.util.Objects.hash(this.name, this.unknownProperties);
    }

    @java.lang.Override
    public boolean equals(java.lang.Object other) {
        if (this == other)
            return true;
        else if (other instanceof rootisref.Other otherOther)
            return java.util.Objects.equals(this.name, otherOther.name) && java.util.Objects.equals(this.unknownProperties, otherOther.unknownProperties);
        else
            return false;
    }
}