/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kubernetes.operator.reconciler.kafkaproxy;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyEgressRule;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyEgressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;

import io.kroxylicious.proxy.config.NetworkRequirements;

/**
 * <p>A factory for Kubernetes {@code NetworkPolicy} resources which will
 * limit a proxy instance's networking based on the addresses in the proxy's configuration.
 *
 * <h1>External DNS names</h1>
 * <p>
 * {@code NetworkPolicies} support IP address, or kube DNS names,
 * but not rules for external DNS names.
 * That's problematic when a proxy is configured with some external endpoint like
 * {@code https://my-kms.example.com/path/to/my/endpoint}.
 * In that case we have two options: Fail, or create a very wide rule which allows egress to any
 * host on the relevant port (443 in the example).
 * <p>
 * If other networking infrastructure is available on the cluster which does support DNS-based rules
 * then it may be possible additionally use some vendor-specific API to configure that so that
 * what's allowed is more strictly limited to the IPs to which those DNS names resolve from
 * the PoV of the proxy pod.
 */
class NetworkPolicyFactory implements NetworkingPolicyFactory<NetworkPolicy> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkPolicyFactory.class);

    /**
     * We use this to group ingress and egress requirements into classes according to how
     * they need to be expressed in the NetworkPolicy API.
     * @param type
     * @param clusterLocal
     */
    record PartitionKey(NetworkRequirements.Egress.Type type,
                        boolean clusterLocal) {
        static PartitionKey INTERNAL_DNS = new PartitionKey(NetworkRequirements.Egress.Type.DNS_NAME, true);
        static PartitionKey EXTERNAL_DNS = new PartitionKey(NetworkRequirements.Egress.Type.DNS_NAME, false);
        static PartitionKey INTERNAL_IP = new PartitionKey(NetworkRequirements.Egress.Type.IP_ADDRESS, true);
        static PartitionKey EXTERNAL_IP = new PartitionKey(NetworkRequirements.Egress.Type.IP_ADDRESS, false);
    }

    @Override
    public NetworkPolicy buildPolicy(Map<String, String> proxyPodSelector,
                                     BiFunction<String, String, Service> proxyServices,
                                     NetworkRequirements proxyNetworkRequirements) {

        var partitioned = proxyNetworkRequirements.egresses().stream()
                .collect(Collectors.groupingBy(egress -> new PartitionKey(
                        egress.type(),
                        egress.isDnsName() ?
                                ClusterDomain.isClusterDomain(egress.host()) :
                                ClusterDomain.isClusterIp(egress.host())
                )));

        List<NetworkRequirements.Egress> internalDnsName = partitioned.getOrDefault(PartitionKey.INTERNAL_DNS, List.of());
        List<NetworkRequirements.Egress> externalDnsName = partitioned.getOrDefault(PartitionKey.EXTERNAL_DNS, List.of());
        List<NetworkRequirements.Egress> internalIp = partitioned.getOrDefault(PartitionKey.INTERNAL_IP, List.of());
        List<NetworkRequirements.Egress> externalIp = partitioned.getOrDefault(PartitionKey.EXTERNAL_IP, List.of());

        if (!internalIp.isEmpty()) {
            // There's no good way to look up a namespace from a cluster-internal IP address.
            // Using an IPBlock rule for internal IP is an anti-pattern.
            // So we disallow this case.
            throw new IllegalArgumentException("Use cluster DNS name to refer to cluster-local pods and services");
        }

        var egressRules = Stream.<NetworkPolicyEgressRule> builder();

        // If the config requires to connect to any cluster-local DNS name we can resolve the namespace (and maybe the pods too)
        internalDnsName.stream().map(egress -> allowToNamespaceAndPod(proxyServices, egress)).forEach(egressRules::add);
        if (!externalDnsName.isEmpty()) {
            // If the config requires to connect to any off-cluster DNS name:
            // 1. we will need to allow DNS
            egressRules.add(allowExternalDns());
            // 2. NetworkPolicy only groks ip addresses, so we need to allow any IP to the matching port.
            externalDnsName.stream().map(NetworkPolicyFactory::allowToAnyIpMatchingPort).forEach(egressRules::add);
        }
        // If the config requires to connect to any off-cluster IP address we allow just that IP and the matching port.
        externalIp.stream().map(NetworkPolicyFactory::buildExternalIpBlockEgressRule).forEach(egressRules::add);

        var ingressRules = proxyNetworkRequirements.ingresses().stream().map(NetworkPolicyFactory::ingressRule).toList();
        // @formatter:off
        return new NetworkPolicyBuilder()
                .withNewSpec()
                    .withPolicyTypes("Ingress", "Egress")
                    .withNewPodSelector()
                        .withMatchLabels(proxyPodSelector) // select the proxy pod
                    .endPodSelector()
                    .addAllToEgress(egressRules.build().toList())
                    .addAllToIngress(ingressRules)
                .endSpec()
                .build();
        // @formatter:on
    }

    private static NetworkPolicyIngressRule ingressRule(NetworkRequirements.Ingress requirement) {
        // @formatter:off
        return new NetworkPolicyIngressRuleBuilder()
                .addNewFrom()
                    .withNewIpBlock()
                        .withCidr(requirement.address())
                    .endIpBlock()
                .endFrom()
                .addNewPort()
                    .withPort(new IntOrString(requirement.port()))
                    .withProtocol("TCP")
                .endPort()
                .build();
        // @formatter:on
    }

    private static NetworkPolicyEgressRule allowExternalDns() {

        // @formatter:off
        return new NetworkPolicyEgressRuleBuilder()
                .addNewPort()
                    .withNewPort(53)
                    .withProtocol("UDP")
                .endPort()
                .addNewPort()
                    .withNewPort(53)
                    .withProtocol("TCP")
                .endPort()
                // No `to` -> anywhere
            .build();
        // @formatter:on
    }

    private static NetworkPolicyEgressRule allowToAnyIpMatchingPort(NetworkRequirements.Egress egress) {
                        /*
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata:
                 name: allow-all-egress
                spec:
                 podSelector:
                   matchLabels:                                    # Define this based on what pods can talk to everything
                 egress:
                 - ports:
                   - protocol: TCP                                 # You need to define the protocol and port numbers
                     port: target
                 policyTypes:
                 - Egress
                 */
        // @formatter:off
        return new NetworkPolicyEgressRuleBuilder()
                .addNewPort()
                    .withNewPort(egress.port())
                    .withProtocol(egress.protocol())
                .endPort()
                // No `to` -> anywhere
            .build();
        // @formatter:on
    }

    private static NetworkPolicyEgressRule buildExternalIpBlockEgressRule(NetworkRequirements.Egress egress) {
        // @formatter:off
        return new NetworkPolicyEgressRuleBuilder()
                .addNewTo()
                    .withNewIpBlock()
                        .withCidr(egress.host() + (egress.isIpv4() ? "/32" : "/128"))
                    .endIpBlock()
                .endTo()
                .addNewPort()
                    .withPort(new IntOrString(egress.port()))
                    .withProtocol(egress.protocol())
                .endPort()
            .build();
        // @formatter:on
    }

    private static NetworkPolicyEgressRule allowToNamespaceAndPod(BiFunction<String, String, Service> proxyServices,
                                                                  NetworkRequirements.Egress egress) {

        String namespace = ClusterDomain.namespaceFromClusterDnsName(egress.host());
        var peer = new NetworkPolicyPeer();
        if (namespace != null) {
            peer.setNamespaceSelector(new LabelSelectorBuilder()
                    .withMatchLabels(Map.of("kubernetes.io/metadata.name", namespace))
                    .build());

            if (ClusterDomain.isServiceDnsName(egress.host())) {
                String serviceName = ClusterDomain.serviceNameFromClusterDnsName(egress.host());
                Service service = proxyServices.apply(namespace, serviceName);
                if (service == null) {
                    LOGGER.atInfo()
                            .addKeyValue("namespace", namespace)
                            .addKeyValue("serviceName", serviceName)
                            .addKeyValue("host", egress.host())
                            .log("Could not get Service implied by cluster-local egress host; egress policy will lack a pod selector");
                }
                else if (service.getSpec() == null
                        || service.getSpec().getSelector() == null) {
                    LOGGER.atInfo()
                            .addKeyValue("namespace", namespace)
                            .addKeyValue("serviceName", serviceName)
                            .addKeyValue("host", egress.host())
                            .log("Service implied by cluster-local egress host lacked a pod selector; egress policy will lack a pod selector");
                }
                else {
                    peer.setPodSelector(new LabelSelectorBuilder()
                            .withMatchLabels(service.getSpec().getSelector())
                            .build());
                }
            }
        }
        // @formatter:off
        return new NetworkPolicyEgressRuleBuilder()
                .addNewPort()
                    .withProtocol(egress.protocol())
                    .withPort(new IntOrString(egress.port()))
                .endPort()
                .withTo(peer)
            .build();
        // @formatter:on
    }
}
