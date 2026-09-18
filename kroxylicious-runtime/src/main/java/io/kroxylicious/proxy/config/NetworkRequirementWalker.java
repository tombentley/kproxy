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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import io.kroxylicious.proxy.config.admin.ManagementConfiguration;
import io.kroxylicious.proxy.service.HostPort;

import edu.umd.cs.findbugs.annotations.Nullable;

public class NetworkRequirementWalker {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkRequirementWalker.class);

    ObjectMapper mapper = new ObjectMapper();

    /**
     * Walk a configuration and determine its network requirements, in terms of ingress and egress connectivity
     * @param obj The configuration
     */
    @SuppressWarnings({ "deprecate", "removal" })
    public NetworkRequirements walk(Configuration obj) throws Exception {
        var ingressRequirements = new HashSet<NetworkRequirements.Ingress>();
        var egressRequirements = new HashSet<NetworkRequirements.Egress>();

        HashSet<Object> visited = new HashSet<>();
        var filterDefsByName = Optional.ofNullable(obj.filterDefinitions()).orElse(List.of()).stream()
                .collect(Collectors.toMap(NamedFilterDefinition::name, Function.identity()));
        var clusterDefsByName = Optional.ofNullable(obj.clusterDefinitions()).orElse(List.of()).stream()
                .collect(Collectors.toMap(ClusterDefinition::name, Function.identity()));
        var routerDefsByName = Optional.ofNullable(obj.routerDefinitions()).orElse(List.of()).stream()
                .collect(Collectors.toMap(RouterDefinition::name, Function.identity()));

        if (obj.management() != null) {
            addManagementIngress(ingressRequirements,
                    "management",
                    obj.management());
        }

//        for (var clusterDefn : Optional.ofNullable(obj.clusterDefinitions()).orElse(List.of())) {
//            addClusterDefinitionEgresses(egressRequirements, clusterDefn);
//        }
        for (var vc : obj.virtualClusters()) {
            for (var g : vc.gateways()) {
                if (g.portIdentifiesNode() != null) {
                    addIngresses(ingressRequirements,
                            "vc-" + vc.name() + "-portgateway-" + g.name(),
                            g.portIdentifiesNode());
                }
                if (g.sniHostIdentifiesNode() != null) {
                    addIngress(ingressRequirements,
                            "vc-" + vc.name() + "-snigateway-" + g.name(),
                            g.sniHostIdentifiesNode());
                }
            }

            if (vc.targetCluster() != null) {
                addTargetClusterEgresses(egressRequirements, vc.name(), vc.targetCluster());
            }
            walkRecursive(vc.subjectBuilder(),
                    "vc-" + vc.name() + "-subject-builder",
                    visited, ingressRequirements, egressRequirements);

            if (vc.filters() != null) {
                for (var filter : vc.filters()) {
                    NamedFilterDefinition namedFilterDefinition = filterDefsByName.get(filter);
                    walkRecursive(namedFilterDefinition.config(),
                            "filter-" + namedFilterDefinition.name(),
                            visited, ingressRequirements, egressRequirements);
                }
            }

            if (vc.target() != null) {

                RouteTarget target = vc.target();
                if (target.cluster() != null) {
                    ClusterDefinition clusterDefinition = clusterDefsByName.get(target.cluster());
                    walkRecursive(clusterDefinition,
                            "cluster-" + clusterDefinition.name(),
                            visited,
                            ingressRequirements,
                            egressRequirements);
                    addClusterDefinitionEgresses(egressRequirements,
                            "cluster-" + clusterDefinition.name(),
                            clusterDefinition);
                }
                if (target.router() != null) {
                    RouterDefinition routerDefinition = routerDefsByName.get(target.router());
                    for (var router : routerDefinition.routes()) {
                        for (var filter : Optional.ofNullable(router.filters()).orElse(List.of())) {
                            NamedFilterDefinition namedFilterDefinition = filterDefsByName.get(filter);
                            walkRecursive(namedFilterDefinition.config(),
                                    "filter-" + namedFilterDefinition.name(),
                                    visited,
                                    ingressRequirements,
                                    egressRequirements);
                        }
                    }
                    walkRecursive(routerDefinition, "router-" + routerDefinition.name(), visited, ingressRequirements, egressRequirements);
                }
                //walkRecursive(target, visited, ingressRequirements, egressRequirements);
            }
        }


        //walkRecursive(obj, visited, ingressRequirements, egressRequirements);
        return new NetworkRequirements(List.copyOf(ingressRequirements), List.copyOf(egressRequirements));
    }

    private void walkRecursive(@Nullable Object obj,
                               String reason,
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

        BeanDescription beanDesc = mapper.getSerializationConfig().introspect(javaType);

        for (BeanPropertyDefinition prop : beanDesc.findProperties()) {
            if (prop.couldSerialize()) {
                Object value = prop.getAccessor().getValue(obj);
                if (value instanceof URI uri) {
                    addUriEgress(egresses, reason, uri, prop);
                } else if (value instanceof HostPort hostPort) {
                    addHostPortIngress(ingresses, reason, hostPort);
                } else if (value != null) {
                    walkRecursive(value, reason, visited, ingresses, egresses);
                }
            }
        }
    }

    private void addClusterDefinitionEgresses(Set<NetworkRequirements.Egress> egresses,
                                              String reason,
                                              ClusterDefinition clusterDefinition) {
        for (var hostPort : clusterDefinition.toTargetCluster().bootstrapServersList()) {
            // TODO this is only the bootstrap, not the whole topology.
            // TODO for Strimzi, if this is a cluster DNS then we could assume the brokers are in the same subdomain
            // TODO more generally, it's difficult to know for sure.
            // TODO I suppose in the CR (not the proxy config) we could let the user express
            // how to identify the other brokers -- e.g. by listing them (DNS or IP)
            // or saying, "same subdomain as the boostrap"
            egresses.add(new NetworkRequirements.Egress(reason,
                    hostPort.host(),
                    "TCP",
                    hostPort.port()));
        }
    }

    private static void addManagementIngress(Set<NetworkRequirements.Ingress> ingresses,
                                             String reason,
                                             ManagementConfiguration mc) {
        ingresses.add(new NetworkRequirements.Ingress(
                reason,
                mc.getEffectiveBindAddress(),
                mc.getEffectivePort()));
    }

    private static void addTargetClusterEgresses(Set<NetworkRequirements.Egress> egresses,
                                                 String vcName,
                                                 TargetCluster tc) {
        for (var hostPort : tc.bootstrapServersList()) {
            egresses.add(new NetworkRequirements.Egress("vc-" + vcName + "-target-cluster" + tc, hostPort.host(), "TCP", hostPort.port()));
        }
    }

    private static void addIngresses(
            Set<NetworkRequirements.Ingress> ingresses,
                                     String reason,
                                     PortIdentifiesNodeIdentificationStrategy strategy) {
        var port = Optional.ofNullable(strategy.getNodeStartPort()).orElse(strategy.getBootstrapAddress().port() + 1);
        List<NamedRange> nodeIdRanges = strategy.getNodeIdRanges();
        if (nodeIdRanges != null && !nodeIdRanges.isEmpty()) {
            for (var nodeIdRange : nodeIdRanges) {
                for (int nodeId = nodeIdRange.start(); nodeId <= nodeIdRange.end(); nodeId++) {
                    ingresses.add(new NetworkRequirements.Ingress(
                            reason,
                            strategy.getBootstrapAddress().host(),
                            port++));
                }
            }
        }
    }

    private static void addIngress(Set<NetworkRequirements.Ingress> ingresses,
                                   String reason,
                                   SniHostIdentifiesNodeIdentificationStrategy strategy) {
        ingresses.add(new NetworkRequirements.Ingress(
                reason,
                strategy.getBootstrapAddress(),
                strategy.getBootstrapPort()));
    }

    private static void addUriEgress(Set<NetworkRequirements.Egress> egresses,
                                     String reason,
                                     URI uri,
                                     BeanPropertyDefinition prop) {
        if (uri.isAbsolute()
                && uri.getAuthority() != null
                && uri.getHost() != null) {
            int port = uri.getPort();
            if (port == -1) {
                port = defaultPort(uri.getScheme());
            }
            egresses.add(new NetworkRequirements.Egress(
                    reason,
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

    private static void addHostPortIngress(Set<NetworkRequirements.Ingress> ingresses,
                                           String reason,
                                           HostPort hostPort) {
        ingresses.add(new NetworkRequirements.Ingress(reason, hostPort.host(), hostPort.port()));
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
