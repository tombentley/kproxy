/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.kroxylicious.proxy.config.admin.ManagementConfiguration;
import io.kroxylicious.proxy.service.HostPort;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkRequirementWalkerTest {

    @Test
    void test() throws Exception {

        NetworkRequirementWalker networkRequirementWalker = new NetworkRequirementWalker();
        var requirements = networkRequirementWalker.walk(new Configuration(
                new ManagementConfiguration(null, null, null),
                List.of(new ClusterDefinition("my-kafka", "my-kafka-bootstap.kafka.example.com:9092", null)),
                List.of(new NamedFilterDefinition(
                        "my-filter",
                        IngressEgressFilter.class.getName(),
                        new IngressEgressFilter.Config(
                                URI.create("http://foo.example.com:9876"),
                                InetSocketAddress.createUnresolved("0.0.0.0", 99)))),
                List.of(), // default filters
                List.of(), // routers
                List.of(new VirtualCluster("my-vc",
                        null,
                        new RouteTarget("my-kafka", null),
                        List.of(
                                new VirtualClusterGateway("my-gateway",
                                        new PortIdentifiesNodeIdentificationStrategy(
                                                new HostPort("0.0.0.0",
                                                        9092),
                                                "proxy.example.com",
                                                1234,
                                                List.of(new NamedRange("a",
                                                        0,
                                                        3))),
                                        null,
                                        Optional.empty())
                        ),
                        false,
                        false,
                        List.of("my-filter"),
                        null,
                        null,
                        null)),
                null,
                false,
                Optional.empty(),
                null,
                null
                ));

        assertThat(requirements.ingresses()).containsExactlyInAnyOrder(
                new NetworkRequirements.Ingress("management", "0.0.0.0", 9190),
                new NetworkRequirements.Ingress("vc-my-vc-portgateway-my-gateway", "0.0.0.0", 1237),
                new NetworkRequirements.Ingress("vc-my-vc-portgateway-my-gateway", "0.0.0.0", 1236),
                new NetworkRequirements.Ingress("vc-my-vc-portgateway-my-gateway", "0.0.0.0", 1235),
                new NetworkRequirements.Ingress("vc-my-vc-portgateway-my-gateway", "0.0.0.0", 1234));

        assertThat(requirements.egresses()).containsExactlyInAnyOrder(
                new NetworkRequirements.Egress("cluster-my-kafka", "my-kafka-bootstap.kafka.example.com", "TCP", 9092),
                new NetworkRequirements.Egress("filter-my-filter", "foo.example.com", "TCP", 9876)
        );
    }

}