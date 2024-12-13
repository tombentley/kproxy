/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package recursiveref;

/**
 * Auto-generated class representing the schema at /definitions/Bar.
 */
@javax.annotation.processing.Generated("io.kroxylicious.tools.schema.compiler.CodeGen")
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "foo" })
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class Bar {

    @edu.umd.cs.findbugs.annotations.Nullable
    private recursiveref.Foo foo;

    @edu.umd.cs.findbugs.annotations.Nullable
    private java.util.Map<java.lang.String, java.lang.Object> unknownProperties = null;

    /**
     * All properties constructor.
     * @param foo The value of the {@code foo} property. This is an optional property.
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public Bar(@edu.umd.cs.findbugs.annotations.Nullable @com.fasterxml.jackson.annotation.JsonProperty(value = "foo") recursiveref.Foo foo) {
        this.foo = foo;
    }

    /**
     * Return the foo.
     *
     * @return The value of this object's foo.
     */
    @edu.umd.cs.findbugs.annotations.Nullable
    @com.fasterxml.jackson.annotation.JsonProperty(value = "foo")
    public recursiveref.Foo foo() {
        return this.foo;
    }

    /**
     * Set the foo.
     *
     *  @param foo The new value for this object's foo.
     */
    public void foo(@edu.umd.cs.findbugs.annotations.Nullable recursiveref.Foo foo) {
        this.foo = foo;
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
        return "Bar[" + "foo: " + this.foo + this.unknownProperties + "]";
    }

    @java.lang.Override
    public int hashCode() {
        return java.util.Objects.hash(this.foo, this.unknownProperties);
    }

    @java.lang.Override
    public boolean equals(java.lang.Object other) {
        if (this == other)
            return true;
        else if (other instanceof recursiveref.Bar otherBar)
            return java.util.Objects.equals(this.foo, otherBar.foo) && java.util.Objects.equals(this.unknownProperties, otherBar.unknownProperties);
        else
            return false;
    }
}