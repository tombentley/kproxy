/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config;

import java.util.List;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.Nullable;

public record NetworkRequirements(
        List<Ingress> ingresses,
        List<Egress> egresses) {

    public static final Pattern IPV4_PATTERN = Pattern.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");

    /**
     * An ingress requirement to the given address
     * @param address The host, which may be a DNS name, IPv4 address or IPv6 address. IPv6 addresses will be enclosed in {@code [} and {@code ]}
     * @param port The port number, if known
     */
    public record Ingress(
            @Nullable String address,
            @Nullable Integer port) {
    }

    /**
     * An egress requirement to the given host
     * @param host The host, which may be a DNS name, IPv4 address or IPv6 address. IPv6 addresses will be enclosed in {@code [} and {@code ]}
     * @param protocol The network protocol (e.g. TCP)
     * @param port The port number, if known
     */
    public record Egress(
            String host,
            String protocol,
            @Nullable Integer port) {

        public enum Type {
            DNS_NAME,
            IPV4,
            IPV6
        }

        public Type type() {
            if (isIpv6()) {
                return Type.IPV6;
            }
            else if (isIpv4()) {
                return Type.IPV4;
            }
            else {
                return Type.DNS_NAME;
            }
        }

        public boolean isDnsName() {
            return !isIpv6() && !isIpv4();
        }

        public boolean isIpv6() {
            return host.startsWith("[") && host.endsWith("]");
        }

        public boolean isIpv4() {
            return IPV4_PATTERN.matcher(host).matches();
        }
    }
}
