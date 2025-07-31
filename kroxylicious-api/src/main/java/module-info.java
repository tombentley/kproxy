module io.kroxylicious.proxy.api {
    requires com.fasterxml.jackson.annotation;
    requires com.github.spotbugs.annotations;
    requires jsr305;
    requires kafka.clients;
    exports io.kroxylicious.proxy.authentication;
    exports io.kroxylicious.proxy.config.tls;
    exports io.kroxylicious.proxy.config.secret;
    exports io.kroxylicious.proxy.filter;
    exports io.kroxylicious.proxy.filter.filterresultbuilder;
    exports io.kroxylicious.proxy.plugin;
    exports io.kroxylicious.proxy.tls;
}