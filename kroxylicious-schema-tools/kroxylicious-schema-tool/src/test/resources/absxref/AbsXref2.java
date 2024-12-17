/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package absxref;

/**
 * Xref via absolution URIs between resources in the same run of the compiler
 */
@javax.annotation.processing.Generated("io.kroxylicious.tools.schema.compiler.CodeGen")
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "one" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class AbsXref2 {

    @edu.umd.cs.findbugs.annotations.Nullable
    private absxref.AbsXref one;

    @edu.umd.cs.findbugs.annotations.Nullable
    private java.util.Map<java.lang.String, java.lang.Object> unknownProperties = null;

    /**
     * All properties constructor.
     * @param one The value of the {@code one} property. This is an optional property.
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public AbsXref2(@edu.umd.cs.findbugs.annotations.Nullable @com.fasterxml.jackson.annotation.JsonProperty(value = "one") absxref.AbsXref one) {
        this.one = one;
    }

    /**
     * Return the one.
     *
     * @return The value of this object's one.
     */
    @edu.umd.cs.findbugs.annotations.Nullable
    @com.fasterxml.jackson.annotation.JsonProperty(value = "one")
    public absxref.AbsXref one() {
        return this.one;
    }

    /**
     * Xref via absolution URIs between resources in the same run of the compiler
     *  @param one The new value for this object's one.
     */
    public void one(@edu.umd.cs.findbugs.annotations.Nullable absxref.AbsXref one) {
        this.one = one;
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
        return "AbsXref2[" + "one: " + this.one + this.unknownProperties + "]";
    }

    @java.lang.Override
    public int hashCode() {
        return java.util.Objects.hash(this.one, this.unknownProperties);
    }

    @java.lang.Override
    public boolean equals(java.lang.Object other) {
        if (this == other)
            return true;
        else if (other instanceof absxref.AbsXref2 otherAbsXref2)
            return java.util.Objects.equals(this.one, otherAbsXref2.one) && java.util.Objects.equals(this.unknownProperties, otherAbsXref2.unknownProperties);
        else
            return false;
    }
}