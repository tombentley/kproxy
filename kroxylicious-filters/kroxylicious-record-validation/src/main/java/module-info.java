module io.kroxylicious.filter.validation {
    requires apicurio.registry.schema.resolver;
    requires apicurio.registry.schema.validation.jsonschema;
    requires apicurio.registry.serde.common;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.github.spotbugs.annotations;
    requires kafka.clients;
    requires org.slf4j;
    requires kroxylicious.api;
    requires io.kroxylicious.proxy.tag;
}