/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kubernetes.operator.reconciler.kafkaproxy;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;

import io.kroxylicious.proxy.config.NetworkRequirements;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkPolicyFactoryTest {

    @Test
    void testExternalKafkaEgress() throws JsonProcessingException {

        // Given
        var proxyPodSelector = Map.of("pod", "my-proxy");
        Service service = new ServiceBuilder().build();

        // When
        NetworkPolicy networkPolicy = new NetworkPolicyFactory().buildPolicy( proxyPodSelector,
                (namespace, name) -> {
                    return service;
                },
                new NetworkRequirements(
                        List.of(),
                        List.of(new NetworkRequirements.Egress("node1.kafka.example.com", "TCP", 9092)))
                );

        // Then
        assertThat(new YAMLMapper().writeValueAsString(networkPolicy)).isEqualTo("""
                ---
                apiVersion: "networking.k8s.io/v1"
                kind: "NetworkPolicy"
                spec:
                  egress:
                  - ports:
                    - port: 53
                      protocol: "UDP"
                    - port: 53
                      protocol: "TCP"
                  - ports:
                    - port: 9092
                      protocol: "TCP"
                  podSelector:
                    matchLabels:
                      pod: "my-proxy"
                  policyTypes:
                  - "Ingress"
                  - "Egress"
                """);
    }

    @Test
    void testInternalKafkaEgress() throws JsonProcessingException {

        // Given
        var proxyPodSelector = Map.of("pod", "my-proxy");
        Service service = new ServiceBuilder()
                .withNewSpec()
                        .withSelector(Map.of("pod", "kafka-pods"))
                .endSpec()
                .build();
        NetworkRequirements requirements = new NetworkRequirements(
                List.of(),
                List.of(new NetworkRequirements.Egress("my-kafka-bootstrap.my-kafka.svc.cluster.local", "TCP", 9092)));
        BiFunction<String, String, Service> stringStringServiceBiFunction = (namespace, name) -> {
            if (namespace.equals("my-kafka") && name.equals("my-kafka-bootstrap")) {
            }
            return service;
        };

        // When
        NetworkPolicy networkPolicy = new NetworkPolicyFactory().buildPolicy(
                proxyPodSelector,
                stringStringServiceBiFunction,
                requirements
        );

        // Then
        assertThat(new YAMLMapper().writeValueAsString(networkPolicy)).isEqualTo("""
                ---
                apiVersion: "networking.k8s.io/v1"
                kind: "NetworkPolicy"
                spec:
                  egress:
                  - ports:
                    - port: 9092
                      protocol: "TCP"
                    to:
                    - namespaceSelector:
                        matchLabels:
                          kubernetes.io/metadata.name: "my-kafka"
                      podSelector:
                        matchLabels:
                          pod: "kafka-pods"
                  podSelector:
                    matchLabels:
                      pod: "my-proxy"
                  policyTypes:
                  - "Ingress"
                  - "Egress"
                """);
    }

}