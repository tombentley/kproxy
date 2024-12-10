/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package open;

/**
 * An class that allows additional properties
 */
@javax.annotation.processing.Generated("io.kroxylicious.tools.schema.compiler.CodeGen")
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "unconstrained", "constrainedToString", "closed" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class Open {

    @edu.umd.cs.findbugs.annotations.Nullable
    private open.OpenUnconstrained unconstrained;

    @edu.umd.cs.findbugs.annotations.Nullable
    private open.OpenConstrainedToString constrainedToString;

    @edu.umd.cs.findbugs.annotations.Nullable
    private open.OpenClosed closed;

    @edu.umd.cs.findbugs.annotations.Nullable
    private java.util.Map<java.lang.String, java.lang.Object> unknownProperties = null;

    /**
     * All properties constructor.
     * @param unconstrained The value of the {@code unconstrained} property. This is an optional property.
     * @param constrainedToString The value of the {@code constrainedToString} property. This is an optional property.
     * @param closed The value of the {@code closed} property. This is an optional property.
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public Open(@edu.umd.cs.findbugs.annotations.Nullable @com.fasterxml.jackson.annotation.JsonProperty(value = "unconstrained") open.OpenUnconstrained unconstrained, @edu.umd.cs.findbugs.annotations.Nullable @com.fasterxml.jackson.annotation.JsonProperty(value = "constrainedToString") open.OpenConstrainedToString constrainedToString, @edu.umd.cs.findbugs.annotations.Nullable @com.fasterxml.jackson.annotation.JsonProperty(value = "closed") open.OpenClosed closed) {
        this.unconstrained = unconstrained;
        this.constrainedToString = constrainedToString;
        this.closed = closed;
    }

    /**
     * Return the unconstrained.
     *
     * @return The value of this object's unconstrained.
     */
    @edu.umd.cs.findbugs.annotations.Nullable
    @com.fasterxml.jackson.annotation.JsonProperty(value = "unconstrained")
    public open.OpenUnconstrained unconstrained() {
        return this.unconstrained;
    }

    /**
     * Set the unconstrained.
     *
     *  @param unconstrained The new value for this object's unconstrained.
     */
    public void unconstrained(@edu.umd.cs.findbugs.annotations.Nullable open.OpenUnconstrained unconstrained) {
        this.unconstrained = unconstrained;
    }

    /**
     * Return the constrainedToString.
     *
     * @return The value of this object's constrainedToString.
     */
    @edu.umd.cs.findbugs.annotations.Nullable
    @com.fasterxml.jackson.annotation.JsonProperty(value = "constrainedToString")
    public open.OpenConstrainedToString constrainedToString() {
        return this.constrainedToString;
    }

    /**
     * Set the constrainedToString.
     *
     *  @param constrainedToString The new value for this object's constrainedToString.
     */
    public void constrainedToString(@edu.umd.cs.findbugs.annotations.Nullable open.OpenConstrainedToString constrainedToString) {
        this.constrainedToString = constrainedToString;
    }

    /**
     * Return the closed.
     *
     * @return The value of this object's closed.
     */
    @edu.umd.cs.findbugs.annotations.Nullable
    @com.fasterxml.jackson.annotation.JsonProperty(value = "closed")
    public open.OpenClosed closed() {
        return this.closed;
    }

    /**
     * Set the closed.
     *
     *  @param closed The new value for this object's closed.
     */
    public void closed(@edu.umd.cs.findbugs.annotations.Nullable open.OpenClosed closed) {
        this.closed = closed;
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
        return "Open[" + "unconstrained: " + this.unconstrained + ", constrainedToString: " + this.constrainedToString + ", closed: " + this.closed + this.unknownProperties + "]";
    }

    @java.lang.Override
    public int hashCode() {
        return java.util.Objects.hash(this.unconstrained, this.constrainedToString, this.closed, this.unknownProperties);
    }

    @java.lang.Override
    public boolean equals(java.lang.Object other) {
        if (this == other)
            return true;
        else if (other instanceof open.Open otherOpen)
            return java.util.Objects.equals(this.unconstrained, otherOpen.unconstrained) && java.util.Objects.equals(this.constrainedToString, otherOpen.constrainedToString) && java.util.Objects.equals(this.closed, otherOpen.closed) && java.util.Objects.equals(this.unknownProperties, otherOpen.unknownProperties);
        else
            return false;
    }
}