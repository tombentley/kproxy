/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import io.kroxylicious.proxy.config.admin.ManagementConfiguration;
import io.kroxylicious.proxy.service.HostPort;
import io.kroxylicious.proxy.service.NodeIdentificationStrategy;

import edu.umd.cs.findbugs.annotations.Nullable;

public class NetworkRequirementWalker {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkRequirementWalker.class);

    ObjectMapper mapper = new ObjectMapper();

    /**
     * Walk a configuration and determine its network requirements, in terms of ingress and egress connectivity
     * @param obj The configuration
     */
    public NetworkRequirements walk(Configuration obj) throws Exception {
        var ingressRequirements = new HashSet<NetworkRequirements.Ingress>();
        var egressRequirements = new HashSet<NetworkRequirements.Egress>();
        walkRecursive(obj, new HashSet<>(), ingressRequirements, egressRequirements);
        return new NetworkRequirements(List.copyOf(ingressRequirements), List.copyOf(egressRequirements));
    }

    private void walkRecursive(@Nullable Object obj,
                               Set<Object> visited,
                               Set<NetworkRequirements.Ingress> ingresses,
                               Set<NetworkRequirements.Egress> egresses) throws Exception {
        if (obj == null || !visited.add(obj)) return;

        Class<?> clazz = obj.getClass();

        // Let Jackson decide if this type is a container/bean vs. a scalar/primitive
        JavaType javaType = mapper.constructType(clazz);
        if (clazz.getName().startsWith("java.") || Enum.class.isAssignableFrom(clazz)) {
            // Primitive / standard Java scalar — stop recursion here
            return;
        }

        if (obj instanceof ManagementConfiguration mc) {
            addManagementIngress(ingresses, mc);
        }
        if (obj instanceof NodeIdentificationStrategy) {
            if (obj instanceof PortIdentifiesNodeIdentificationStrategy strategy) {
                addIngresses(ingresses, strategy);
            }
            else if (obj instanceof SniHostIdentifiesNodeIdentificationStrategy strategy) {
                addIngress(ingresses, strategy);
            }
            else {
                throw new IllegalArgumentException("Unknown node identification strategy: " + clazz.getName());
            }
        }
        if (obj instanceof TargetCluster tc) {
            addTargetClusterEgresses(egresses, tc);
        }

        BeanDescription beanDesc = mapper.getSerializationConfig().introspect(javaType);

        for (BeanPropertyDefinition prop : beanDesc.findProperties()) {
            if (prop.couldSerialize()) {
                Object value = prop.getAccessor().getValue(obj);
                if (value instanceof URI uri) {
                    addUriEgress(egresses, uri, prop);
                } else if (value instanceof HostPort hostPort) {
                    addHostPortIngress(ingresses, hostPort);
                } else if (value != null) {
                    walkRecursive(value, visited, ingresses, egresses);
                }
            }
        }
    }

    private static void addManagementIngress(Set<NetworkRequirements.Ingress> ingresses, ManagementConfiguration mc) {
        ingresses.add(new NetworkRequirements.Ingress(mc.bindAddress(), mc.getEffectivePort()));
    }

    private static void addTargetClusterEgresses(Set<NetworkRequirements.Egress> egresses, TargetCluster tc) {
        for (var hostPort : tc.bootstrapServersList()) {
            egresses.add(new NetworkRequirements.Egress(hostPort.host(), "TCP", hostPort.port()));
        }
    }

    private static void addIngresses(Set<NetworkRequirements.Ingress> ingresses, PortIdentifiesNodeIdentificationStrategy strategy) {
        var port = Optional.ofNullable(strategy.getNodeStartPort()).orElse(strategy.getBootstrapAddress().port() + 1);
        List<NamedRange> nodeIdRanges = strategy.getNodeIdRanges();
        if (nodeIdRanges != null && !nodeIdRanges.isEmpty()) {
            for (var nodeIdRange : nodeIdRanges) {
                for (int nodeId = nodeIdRange.start(); nodeId <= nodeIdRange.end(); nodeId++) {
                    ingresses.add(new NetworkRequirements.Ingress(strategy.getBootstrapAddress().host(), port++));
                }
            }
        }
    }

    private static void addIngress(Set<NetworkRequirements.Ingress> ingresses, SniHostIdentifiesNodeIdentificationStrategy strategy) {
        ingresses.add(new NetworkRequirements.Ingress(strategy.getBootstrapAddress(), strategy.getBootstrapPort()));
    }

    private static void addUriEgress(Set<NetworkRequirements.Egress> egresses, URI uri, BeanPropertyDefinition prop) {
        if (uri.isAbsolute()
                && uri.getAuthority() != null
                && uri.getHost() != null) {
            int port = uri.getPort();
            if (port == -1) {
                port = defaultPort(uri.getScheme());
            }
            egresses.add(new NetworkRequirements.Egress(
                    uri.getHost(),
                    "TCP",
                    port != -1 ? port : null));
        }
        else {
            LOGGER.atDebug()
                    .addKeyValue("URI", uri)
                    .addKeyValue("property", prop.getName())
                    .log("Ignoring URI because it lacks a host");
        }
    }

    private static void addHostPortIngress(Set<NetworkRequirements.Ingress> ingresses, HostPort hostPort) {
        ingresses.add(new NetworkRequirements.Ingress(hostPort.host(), hostPort.port()));
    }

    private static final Map<String, Integer> DEFAULT_PORTS = Map.ofEntries(
            Map.entry("http", 80),
            Map.entry("https", 443),
            Map.entry("ws", 80),
            Map.entry("wss", 443),
            Map.entry("ftp", 21),
            Map.entry("ssh", 22),
            Map.entry("sftp", 22),
            Map.entry("smtp", 25),
            Map.entry("smtps", 465),
            Map.entry("postgres", 5432),
            Map.entry("mysql", 3306),
            Map.entry("mongodb", 27017)
    );

    public static int defaultPort(@Nullable String scheme) {
        if (scheme == null) return -1;
        return DEFAULT_PORTS.getOrDefault(scheme.toLowerCase(Locale.ENGLISH), -1);
    }
}
