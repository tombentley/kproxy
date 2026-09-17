/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kubernetes.operator.reconciler.kafkaproxy;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.Nullable;

public class ClusterDomain {

    public final static String CLUSTER_DOMAIN;
    static {
        String clusterDomain;
        String envVarValue = System.getenv("CLUSTER_DOMAIN");
        if (envVarValue != null) {
            clusterDomain = envVarValue;
        } else {
            try (var stream = Files.lines(Path.of("/etc/resolv.conf"))) {
                Optional<String> searchListOpt = stream
                        .filter(line -> line.startsWith("search "))
                        // manpage says: "If there are multiple search directives, only the search list from the last instance is used."
                        .reduce((existing, lastest) -> lastest);
                clusterDomain = searchListOpt.flatMap(
                        searchList -> Arrays.stream(searchList.trim().split(" +"))
                                .filter(n -> n.startsWith("svc."))
                                .findFirst())
                        .orElse("cluster.local");
            }
            catch (Exception e) {
                clusterDomain = "cluster.local";
            }
        }
        CLUSTER_DOMAIN = clusterDomain;
    }

    // matches service DNS names
    // IP-based pod DNS names
    // hostname and subdomain pod DNS names
    private static final Pattern COMPILE = Pattern.compile(".*\\.(a-z0-9-+)\\.(a-z0-9-+)\\.(svc|pod)\\." + Pattern.quote(CLUSTER_DOMAIN) + "$");

    /**
     * Determines whether the given DNS name ends with the cluster domain suffix.
     * @param dnsName The DNS name
     * @return true iff the given DNS name ends with the cluster domain suffix.
     */
    static boolean isClusterDomain(String dnsName) {
        return dnsName.endsWith("." + CLUSTER_DOMAIN);
    }

    /**
     * Extracts the namespace name from a cluster DNS name.
     * This works for both Service DNS names (ending {@code .svc.<cluster-domain>}) and Pod DNS names
     * (ending {@code .pod.<cluster-domain>}).
     * @param clusterDnsName The DNS name
     * @return the namespace, or null if the given DNS name was not a cluster DNS name
     */
    static @Nullable String namespaceFromClusterDnsName(String clusterDnsName) {
        Matcher matcher = COMPILE.matcher(clusterDnsName);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
    }

    /**
     * Extracts the DNS name type from a cluster DNS name.
     * This Service DNS names (ending {@code .svc.<cluster-domain>}) it will return "svc".
     * For Pod DNS names (ending {@code .pod.<cluster-domain>}). it will return "pod".
     * @param clusterDnsName The DNS name
     * @return the type, or null if the given DNS name was not a cluster DNS name
     */
    private static @Nullable String dnsNameType(String clusterDnsName) {
        Matcher matcher = COMPILE.matcher(clusterDnsName);
        if (matcher.matches()) {
            return matcher.group(3);
        }
        return null;
    }

    /**
     * Determine whether the given DNS name is a name for a Pod on this cluster.
     * @param clusterDnsName The DNS name
     * @return true if the name is for a Pod on this cluster.
     */
    static boolean isPodDnsName(String clusterDnsName) {
        return "pod".equals(dnsNameType(clusterDnsName));
    }

    /**
     * Determine whether the given DNS name is a name for a Service on this cluster.
     * @param clusterDnsName The DNS name
     * @return true if the name is for a Service on this cluster.
     */
    static boolean isServiceDnsName(String clusterDnsName) {
        return "svc".equals(dnsNameType(clusterDnsName));
    }

    /**
     * Returns the name of the service which has (or would have) the given service DNS name.
     * @param clusterDnsName The DNS name
     * @return The name of the service, or null if the DNS name is not for a service on this cluster.
     */
    public static @Nullable String serviceNameFromClusterDnsName(String clusterDnsName) {
        Matcher matcher = COMPILE.matcher(clusterDnsName);
        if (matcher.matches() && "svc".equals(matcher.group(3))) {
            return matcher.group(1);
        }
        return null;
    }

    record Cidr(byte[] prefixAddr, int prefixLength) {
        static Cidr fromString(String cidr) {
            var parts = cidr.split("/");
            InetAddress prefixAddr = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);
            return new Cidr(prefixAddr.getAddress(), prefixLength);
        }

        private boolean matches(byte[] ipAddress) {
            int b = prefixLength / 8;
            for (int i = 0; i < b; i++) {
                if (ipAddress[i] != prefixAddr[i]) {
                    return false;
                }
            }
            int c = prefixLength % 8;
            return ipAddress[b+1] == ((prefixAddr[b+1] >> c) << c);
        }

        boolean matches(String ipAddress) {
            try {
                return matches(InetAddress.getByName(ipAddress).getAddress());
            }
            catch (UnknownHostException e) {
                // Should be impossible, if the ipAddress is an IP address
                return false;
            }
        }

    }

    private static List<Cidr> clusterCidr() {
        return List.of(Cidr.fromString("10.22.0.0/16"));
    }

    static boolean isClusterIp(String clusterDnsName) {
        for (Cidr cidr : clusterCidr()) {
            if (cidr.matches(clusterDnsName)) {
                return true;
            }
        }
        return false;
    }
}
