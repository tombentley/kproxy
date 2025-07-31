module io.kroxylicious.filter.encryption.test {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.github.spotbugs.annotations;
    requires com.google.common;
    requires kroxylicious.kms;
    requires kroxylicious.api;
    requires kroxylicious.kafka.message.tools;
    requires kroxylicious.filter.test.support;
    requires kroxylicious.kms.provider.kroxylicious.inmemory;
    requires io.kroxylicious.filter.encryption;
    requires kafka.clients;
    requires micrometer.core;
    requires org.assertj.core;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
    requires org.mockito;
    requires org.mockito.junit.jupiter;

    opens io.kroxylicious.filter.encryption.test to org.junit.platform.commons;
    opens io.kroxylicious.filter.encryption.test.common to org.junit.platform.commons;
    opens io.kroxylicious.filter.encryption.test.config to org.junit.platform.commons;
    opens io.kroxylicious.filter.encryption.test.crypto to org.junit.platform.commons;

    opens io.kroxylicious.filter.encryption.test.decrypt to org.junit.platform.commons;
    opens io.kroxylicious.filter.encryption.test.dek to org.junit.platform.commons;
    opens io.kroxylicious.filter.encryption.test.encrypt to org.junit.platform.commons;
    opens io.kroxylicious.filter.encryption.test.kms to org.junit.platform.commons;
}