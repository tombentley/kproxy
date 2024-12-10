package io.kroxylicious;

import io.kroxylicious.proxy.config.v1.Backend;
import io.kroxylicious.proxy.config.v1.ProxyConfig;
import io.kroxylicious.proxy.config.v1.ProxyConfigTransport;
import io.kroxylicious.proxy.config.v1.ProxyConfigTransportTls;
import io.kroxylicious.proxy.config.v1.ProxyConfigTransportTlsTrustedCertificates;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class Main {

    static <T> Optional<T> path(ProxyConfig root,
                                Function<ProxyConfig, Optional<T>> fn) {
        return fn.apply(root);
    }

    static <T1, T2> Optional<T2> path(ProxyConfig root,
                                      Function<ProxyConfig, Optional<T1>> fn1,
                                      Function<T1, Optional<T2>> fn2) {
        return fn1.apply(root).flatMap(fn2);
    }

    static <T1, T2, T3> Optional<T3> path(ProxyConfig root,
                                          Function<ProxyConfig, Optional<T1>> fn1,
                                          Function<T1, Optional<T2>> fn2,
                                          Function<T2, Optional<T3>> fn3) {
        return fn1.apply(root).flatMap(fn2).flatMap(fn3);
    }

    public static void main(String[] args) {
        // Builder
        // Visitor (use builder's visitor?)
        // JsonAnyGetter and AnySetter
        // Optional
        var pc = new ProxyConfig(null, null, null, null);

        var certs = pc.optBackends().stream()
                .flatMap(m -> m.values().stream())
                .flatMap(b -> b.optTransport().stream())
                .flatMap(b -> b.optTls().stream())
                .flatMap(b -> b.optTrustedCertificates().stream())
                .flatMap(b -> b.optFromFile().stream());
        var files = certs.toList();
    }
}
