/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kubernetes.operator.reconciler.kafkaproxy;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ovn.v1.EgressFirewall;
import io.fabric8.kubernetes.api.model.ovn.v1.EgressFirewallBuilder;
import io.fabric8.openshift.api.model.Route;

import io.kroxylicious.proxy.config.NetworkRequirements;

/**
 * Traffic is evaluated by {@code NetworkPolicy} rules first at the pod network level,
 * and then by {@code EgressFirewall} rules at the cluster egress gateway level.
 * Because this applies only to traffic exiting the cluster we shoud omit rules
 * for intra-cluster connections.
 */
class OvnEgressFirewallFactory implements NetworkingPolicyFactory<EgressFirewall> {

    @Override
    public EgressFirewall buildPolicy(Map<String, String> proxyPodSelector,
                                      BiFunction<String, String, Service> proxyServices,
                                      NetworkRequirements proxyNetworkRequirements) {
        // @formatter:off
        return new EgressFirewallBuilder()
                .withNewMetadata()
                    .withName("default")
                    .withNamespace("my-namespace")
                .endMetadata()
                .withNewSpec()
                    .addNewEgress()
                        .withType("Allow")
                        .withNewTo()
                            .withDnsName("203.0.113.0/24")
                        .endTo()
                    .endEgress()
                .endSpec()
            .build();
        // @formatter:on
    }
}
