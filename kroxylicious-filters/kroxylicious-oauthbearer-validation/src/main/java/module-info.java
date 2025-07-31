module io.kroxylicious.filter.oauthbearer {
    requires com.fasterxml.jackson.annotation;
    requires com.github.benmanes.caffeine;
    requires com.github.spotbugs.annotations;
    requires java.security.sasl;
    requires kafka.clients;
    requires org.slf4j;
    requires io.kroxylicious.proxy.tag;
    requires io.kroxylicious.proxy.api;
}