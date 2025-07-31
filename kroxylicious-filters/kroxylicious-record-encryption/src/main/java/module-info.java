module io.kroxylicious.filter.encryption {
    requires com.fasterxml.jackson.annotation;
    requires com.github.benmanes.caffeine;
    requires com.github.spotbugs.annotations;
    requires jsr305;
    requires kafka.clients;
    requires micrometer.core;
    requires org.slf4j;
    requires kroxylicious.kms;
    requires io.kroxylicious.proxy.api;
    requires io.kroxylicious.proxy.tag;
    requires kroxylicious.kafka.message.tools;
}