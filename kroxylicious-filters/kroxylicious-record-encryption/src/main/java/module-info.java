module io.kroxylicious.filter.encryption {
    requires com.fasterxml.jackson.annotation;
    requires com.github.benmanes.caffeine;
    requires com.github.spotbugs.annotations;
    requires jsr305;
    requires kafka.clients;
    requires micrometer.core;
    requires org.slf4j;
    requires kroxylicious.kms;
    requires kroxylicious.api;
    requires kroxylicious.kafka.message.tools;
    requires io.kroxylicious.proxy.tag;

    exports io.kroxylicious.filter.encryption to io.kroxylicious.filter.encryption.test;
    exports io.kroxylicious.filter.encryption.common to io.kroxylicious.filter.encryption.test;
    exports io.kroxylicious.filter.encryption.config to io.kroxylicious.filter.encryption.test;
    exports io.kroxylicious.filter.encryption.crypto to io.kroxylicious.filter.encryption.test;
    exports io.kroxylicious.filter.encryption.decrypt to io.kroxylicious.filter.encryption.test;
    exports io.kroxylicious.filter.encryption.dek to io.kroxylicious.filter.encryption.test;
    exports io.kroxylicious.filter.encryption.encrypt to io.kroxylicious.filter.encryption.test;
    exports io.kroxylicious.filter.encryption.kms to io.kroxylicious.filter.encryption.test;
}