/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package recursiveref;

/**
 * Auto-generated class representing the schema at /definitions/Foo.
 */
@javax.annotation.processing.Generated("io.kroxylicious.tools.schema.compiler.CodeGen")
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "bar", "barElsewhere" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class Foo {

    @edu.umd.cs.findbugs.annotations.Nullable
    private recursiveref.Bar bar;

    @edu.umd.cs.findbugs.annotations.Nullable
    private recursiveref.BarElsewhere barElsewhere;

    @edu.umd.cs.findbugs.annotations.Nullable
    private java.util.Map<java.lang.String, java.lang.Object> unknownProperties = null;

    /**
     * All properties constructor.
     * @param bar The value of the {@code bar} property. This is an optional property.
     * @param barElsewhere The value of the {@code barElsewhere} property. This is an optional property.
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public Foo(@edu.umd.cs.findbugs.annotations.Nullable @com.fasterxml.jackson.annotation.JsonProperty(value = "bar") recursiveref.Bar bar, @edu.umd.cs.findbugs.annotations.Nullable @com.fasterxml.jackson.annotation.JsonProperty(value = "barElsewhere") recursiveref.BarElsewhere barElsewhere) {
        this.bar = bar;
        this.barElsewhere = barElsewhere;
    }

    /**
     * Return the bar.
     *
     * @return The value of this object's bar.
     */
    @edu.umd.cs.findbugs.annotations.Nullable
    @com.fasterxml.jackson.annotation.JsonProperty(value = "bar")
    public recursiveref.Bar bar() {
        return this.bar;
    }

    /**
     * Set the bar.
     *
     *  @param bar The new value for this object's bar.
     */
    public void bar(@edu.umd.cs.findbugs.annotations.Nullable recursiveref.Bar bar) {
        this.bar = bar;
    }

    /**
     * Return the barElsewhere.
     *
     * @return The value of this object's barElsewhere.
     */
    @edu.umd.cs.findbugs.annotations.Nullable
    @com.fasterxml.jackson.annotation.JsonProperty(value = "barElsewhere")
    public recursiveref.BarElsewhere barElsewhere() {
        return this.barElsewhere;
    }

    /**
     * Set the barElsewhere.
     *
     *  @param barElsewhere The new value for this object's barElsewhere.
     */
    public void barElsewhere(@edu.umd.cs.findbugs.annotations.Nullable recursiveref.BarElsewhere barElsewhere) {
        this.barElsewhere = barElsewhere;
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
        return "Foo[" + "bar: " + this.bar + ", barElsewhere: " + this.barElsewhere + this.unknownProperties + "]";
    }

    @java.lang.Override
    public int hashCode() {
        return java.util.Objects.hash(this.bar, this.barElsewhere, this.unknownProperties);
    }

    @java.lang.Override
    public boolean equals(java.lang.Object other) {
        if (this == other)
            return true;
        else if (other instanceof recursiveref.Foo otherFoo)
            return java.util.Objects.equals(this.bar, otherFoo.bar) && java.util.Objects.equals(this.barElsewhere, otherFoo.barElsewhere) && java.util.Objects.equals(this.unknownProperties, otherFoo.unknownProperties);
        else
            return false;
    }
}