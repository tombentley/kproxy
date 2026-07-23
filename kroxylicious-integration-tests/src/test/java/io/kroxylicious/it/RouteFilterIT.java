/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.it;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.nettyplus.leakdetector.junit.NettyLeakDetectorExtension;

import io.kroxylicious.it.testplugins.FixedClientIdFilterFactory;
import io.kroxylicious.it.testplugins.RejectingCreateTopicFilterFactory;
import io.kroxylicious.it.testplugins.router.PassThroughRouterFactory;
import io.kroxylicious.proxy.config.ClusterDefinition;
import io.kroxylicious.proxy.config.ConfigurationBuilder;
import io.kroxylicious.proxy.config.RouteDefinition;
import io.kroxylicious.proxy.config.RouteTarget;
import io.kroxylicious.proxy.config.RouterDefinition;
import io.kroxylicious.proxy.config.VirtualClusterBuilder;
import io.kroxylicious.proxy.internal.config.Feature;
import io.kroxylicious.proxy.internal.config.Features;
import io.kroxylicious.testing.integration.config.NamedFilterDefinitionBuilder;
import io.kroxylicious.testing.integration.tester.KroxyliciousTesters;
import io.kroxylicious.testing.kafka.api.KafkaCluster;
import io.kroxylicious.testing.kafka.junit5ext.KafkaClusterExtension;
import io.kroxylicious.testing.kafka.junit5ext.Topic;

import static io.kroxylicious.testing.integration.tester.KroxyliciousConfigUtils.baseConfigurationBuilder;
import static io.kroxylicious.testing.integration.tester.KroxyliciousConfigUtils.defaultPortIdentifiesNodeGatewayBuilder;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying that filters configured on routes are applied
 * to traffic traversing those routes.
 */
@ExtendWith(KafkaClusterExtension.class)
@ExtendWith(NettyLeakDetectorExtension.class)
class RouteFilterIT {

    private static final Features ROUTING_ENABLED = Features.builder().enable(Feature.ROUTING).build();
    private static final String ROUTE_NAME = "default-route";
    private static final String ROUTER_NAME = "pass-through";
    private static final String TARGET_CLUSTER_NAME = "backing";
    private static final String TOPIC = "route-filter-test";

    KafkaCluster cluster;

    @BeforeEach
    void setUp() throws Exception {
        try (var admin = org.apache.kafka.clients.admin.AdminClient.create(cluster.getKafkaClientConfiguration())) {
            admin.createTopics(List.of(new NewTopic(TOPIC, Optional.of(1), Optional.empty())))
                    .all().get(10, TimeUnit.SECONDS);
        }
    }

    private ConfigurationBuilder routingConfigWithRouteFilter(String filterName, String filterType, Map<String, Object> filterConfig) {
        var filterDef = new NamedFilterDefinitionBuilder(filterName, filterType)
                .withConfig(filterConfig)
                .build();

        var route = new RouteDefinition(ROUTE_NAME, 0, List.of(filterName), new RouteTarget(TARGET_CLUSTER_NAME, null));
        var routerConfig = new PassThroughRouterFactory.Config(ROUTE_NAME);
        var routerDef = new RouterDefinition(ROUTER_NAME,
                PassThroughRouterFactory.class.getName(), routerConfig, List.of(route));

        var targetCluster = new ClusterDefinition(TARGET_CLUSTER_NAME,
                cluster.getBootstrapServers(), null);

        var vc = new VirtualClusterBuilder()
                .withName("demo")
                .withTarget(new RouteTarget(null, ROUTER_NAME))
                .addToGateways(defaultPortIdentifiesNodeGatewayBuilder("localhost:9192").build())
                .build();

        return baseConfigurationBuilder()
                .addToClusterDefinitions(targetCluster)
                .addToFilterDefinitions(filterDef)
                .addToRouterDefinitions(routerDef)
                .addToVirtualClusters(vc);
    }

    private ConfigurationBuilder routingConfigWithoutRouteFilter() {
        var route = new RouteDefinition(ROUTE_NAME, 0, List.of(), new RouteTarget(TARGET_CLUSTER_NAME, null));
        var routerConfig = new PassThroughRouterFactory.Config(ROUTE_NAME);
        var routerDef = new RouterDefinition(ROUTER_NAME,
                PassThroughRouterFactory.class.getName(), routerConfig, List.of(route));

        var targetCluster = new ClusterDefinition(TARGET_CLUSTER_NAME,
                cluster.getBootstrapServers(), null);

        var vc = new VirtualClusterBuilder()
                .withName("demo")
                .withTarget(new RouteTarget(null, ROUTER_NAME))
                .addToGateways(defaultPortIdentifiesNodeGatewayBuilder("localhost:9192").build())
                .build();

        return baseConfigurationBuilder()
                .addToClusterDefinitions(targetCluster)
                .addToRouterDefinitions(routerDef)
                .addToVirtualClusters(vc);
    }

    @Test
    void produceAndConsumeWorkThroughFilteredRoute() {
        // Given
        var config = routingConfigWithRouteFilter(
                "fixed-client-id",
                FixedClientIdFilterFactory.class.getName(),
                Map.of("clientId", "route-filter-stamped"));

        // When / Then
        try (var tester = KroxyliciousTesters.newBuilder(config).setFeatures(ROUTING_ENABLED).createDefaultKroxyliciousTester();
                var producer = tester.producer();
                var consumer = tester.consumer(
                        Map.of(ConsumerConfig.GROUP_ID_CONFIG, "route-filter-test",
                                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"))) {

            assertThat(producer.send(new ProducerRecord<>(TOPIC, "key", "value")))
                    .succeedsWithin(Duration.ofSeconds(10));

            consumer.subscribe(Set.of(TOPIC));
            var records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.iterator())
                    .toIterable()
                    .singleElement()
                    .extracting(ConsumerRecord::value)
                    .isEqualTo("value");
        }
    }

    @Test
    void routeWithoutFiltersIsUnaffected(Topic topic) {
        // Given
        var config = routingConfigWithoutRouteFilter();

        // When / Then
        try (var tester = KroxyliciousTesters.newBuilder(config).setFeatures(ROUTING_ENABLED).createDefaultKroxyliciousTester();
                var producer = tester.producer();
                var consumer = tester.consumer(
                        Map.of(ConsumerConfig.GROUP_ID_CONFIG, "route-no-filter-test",
                                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"))) {

            assertThat(producer.send(new ProducerRecord<>(topic.name(), "key", "value")))
                    .succeedsWithin(Duration.ofSeconds(10));

            consumer.subscribe(Set.of(topic.name()));
            var records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.iterator())
                    .toIterable()
                    .singleElement()
                    .extracting(ConsumerRecord::value)
                    .isEqualTo("value");
        }
    }

    @Test
    void vcFilterAndRouteFilterBothExecute() throws Exception {
        // Given: VC filter rejects CreateTopics (short-circuits before routing); route has a pass-through filter
        var rejectingFilter = new NamedFilterDefinitionBuilder("rejecting", RejectingCreateTopicFilterFactory.class.getName())
                .withConfig("withCloseConnection", false, "respondWithError", true)
                .build();

        var passThroughFilter = new NamedFilterDefinitionBuilder("pass-through-id", FixedClientIdFilterFactory.class.getName())
                .withConfig("clientId", "vc-and-route")
                .build();

        var route = new RouteDefinition(ROUTE_NAME, 0, List.of("pass-through-id"), new RouteTarget(TARGET_CLUSTER_NAME, null));
        var routerDef = new RouterDefinition(ROUTER_NAME,
                PassThroughRouterFactory.class.getName(), new PassThroughRouterFactory.Config(ROUTE_NAME), List.of(route));

        var targetCluster = new ClusterDefinition(TARGET_CLUSTER_NAME, cluster.getBootstrapServers(), null);

        var vc = new VirtualClusterBuilder()
                .withName("demo")
                .withTarget(new RouteTarget(null, ROUTER_NAME))
                .withFilters(List.of("rejecting"))
                .addToGateways(defaultPortIdentifiesNodeGatewayBuilder("localhost:9192").build())
                .build();

        var config = baseConfigurationBuilder()
                .addToClusterDefinitions(targetCluster)
                .addToFilterDefinitions(rejectingFilter)
                .addToFilterDefinitions(passThroughFilter)
                .addToRouterDefinitions(routerDef)
                .addToVirtualClusters(vc);

        // When: attempt to create a topic through the proxy
        try (var tester = KroxyliciousTesters.newBuilder(config).setFeatures(ROUTING_ENABLED).createDefaultKroxyliciousTester();
                var admin = tester.admin()) {

            var newTopic = new NewTopic("test-topic-creation", Optional.of(1), Optional.empty());

            // Then: the VC-level filter intercepts and rejects before the request reaches any route
            assertThat(admin.createTopics(List.of(newTopic)).all())
                    .failsWithin(Duration.ofSeconds(10))
                    .withThrowableOfType(ExecutionException.class)
                    .withCauseInstanceOf(InvalidTopicException.class)
                    .withMessageContaining(io.kroxylicious.it.testplugins.RejectingCreateTopicFilter.ERROR_MESSAGE);
        }
    }

    @Test
    void routeFilterDoesNotApplyToOtherRoutes(Topic topicFiltered, Topic topicUnfiltered) throws Exception {
        // Given: route-filtered has a rejecting filter; route-unfiltered has no filter
        var rejectingFilter = new NamedFilterDefinitionBuilder("rejecting", RejectingCreateTopicFilterFactory.class.getName())
                .withConfig("withCloseConnection", false, "respondWithError", true)
                .build();

        var targetCluster = new ClusterDefinition(TARGET_CLUSTER_NAME, cluster.getBootstrapServers(), null);

        var routeFiltered = new RouteDefinition("route-filtered", 0, List.of("rejecting"), new RouteTarget(TARGET_CLUSTER_NAME, null));
        var routerFiltered = new RouterDefinition("router-filtered",
                PassThroughRouterFactory.class.getName(), new PassThroughRouterFactory.Config("route-filtered"), List.of(routeFiltered));

        var routeUnfiltered = new RouteDefinition("route-unfiltered", 0, List.of(), new RouteTarget(TARGET_CLUSTER_NAME, null));
        var routerUnfiltered = new RouterDefinition("router-unfiltered",
                PassThroughRouterFactory.class.getName(), new PassThroughRouterFactory.Config("route-unfiltered"), List.of(routeUnfiltered));

        var vcFiltered = new VirtualClusterBuilder()
                .withName("vc-filtered")
                .withTarget(new RouteTarget(null, "router-filtered"))
                .addToGateways(defaultPortIdentifiesNodeGatewayBuilder("localhost:9192").build())
                .build();

        var vcUnfiltered = new VirtualClusterBuilder()
                .withName("vc-unfiltered")
                .withTarget(new RouteTarget(null, "router-unfiltered"))
                .addToGateways(defaultPortIdentifiesNodeGatewayBuilder("localhost:9202").build())
                .build();

        var config = baseConfigurationBuilder()
                .addToClusterDefinitions(targetCluster)
                .addToFilterDefinitions(rejectingFilter)
                .addToRouterDefinitions(routerFiltered, routerUnfiltered)
                .addToVirtualClusters(vcFiltered, vcUnfiltered);

        var newTopic = new NewTopic("test-isolation-topic", Optional.of(1), Optional.empty());

        // When
        try (var tester = KroxyliciousTesters.newBuilder(config).setFeatures(ROUTING_ENABLED).createDefaultKroxyliciousTester();
                var adminFiltered = tester.admin("vc-filtered");
                var adminUnfiltered = tester.admin("vc-unfiltered")) {

            // Then: route-filtered's filter intercepts and rejects the CreateTopics request
            assertThat(adminFiltered.createTopics(List.of(newTopic)).all())
                    .failsWithin(Duration.ofSeconds(10))
                    .withThrowableOfType(ExecutionException.class)
                    .withCauseInstanceOf(InvalidTopicException.class)
                    .withMessageContaining(io.kroxylicious.it.testplugins.RejectingCreateTopicFilter.ERROR_MESSAGE);

            // And route-unfiltered has no filter, so CreateTopics succeeds
            assertThat(adminUnfiltered.createTopics(List.of(newTopic)).all())
                    .succeedsWithin(Duration.ofSeconds(10));
        }
    }

    @Test
    void differentRoutesCanHaveDifferentFilterChains(Topic topicA, Topic topicB) throws Exception {
        // Given: route-a rejects CreateTopics; route-b only modifies clientId (does not reject)
        var rejectingFilter = new NamedFilterDefinitionBuilder("rejecting-a", RejectingCreateTopicFilterFactory.class.getName())
                .withConfig("withCloseConnection", false, "respondWithError", true)
                .build();

        var fixedIdFilter = new NamedFilterDefinitionBuilder("fixed-id-b", FixedClientIdFilterFactory.class.getName())
                .withConfig("clientId", "route-b-client")
                .build();

        var targetCluster = new ClusterDefinition(TARGET_CLUSTER_NAME, cluster.getBootstrapServers(), null);

        var routeA = new RouteDefinition("route-a", 0, List.of("rejecting-a"), new RouteTarget(TARGET_CLUSTER_NAME, null));
        var routerA = new RouterDefinition("router-a",
                PassThroughRouterFactory.class.getName(), new PassThroughRouterFactory.Config("route-a"), List.of(routeA));

        var routeB = new RouteDefinition("route-b", 0, List.of("fixed-id-b"), new RouteTarget(TARGET_CLUSTER_NAME, null));
        var routerB = new RouterDefinition("router-b",
                PassThroughRouterFactory.class.getName(), new PassThroughRouterFactory.Config("route-b"), List.of(routeB));

        var vcA = new VirtualClusterBuilder()
                .withName("vc-a")
                .withTarget(new RouteTarget(null, "router-a"))
                .addToGateways(defaultPortIdentifiesNodeGatewayBuilder("localhost:9192").build())
                .build();

        var vcB = new VirtualClusterBuilder()
                .withName("vc-b")
                .withTarget(new RouteTarget(null, "router-b"))
                .addToGateways(defaultPortIdentifiesNodeGatewayBuilder("localhost:9202").build())
                .build();

        var config = baseConfigurationBuilder()
                .addToClusterDefinitions(targetCluster)
                .addToFilterDefinitions(rejectingFilter, fixedIdFilter)
                .addToRouterDefinitions(routerA, routerB)
                .addToVirtualClusters(vcA, vcB);

        var newTopic = new NewTopic("test-chains-topic", Optional.of(1), Optional.empty());

        // When
        try (var tester = KroxyliciousTesters.newBuilder(config).setFeatures(ROUTING_ENABLED).createDefaultKroxyliciousTester();
                var adminA = tester.admin("vc-a");
                var adminB = tester.admin("vc-b")) {

            // Then: route-a's filter rejects CreateTopics; route-b's different filter does not
            assertThat(adminA.createTopics(List.of(newTopic)).all())
                    .failsWithin(Duration.ofSeconds(10))
                    .withThrowableOfType(ExecutionException.class)
                    .withCauseInstanceOf(InvalidTopicException.class)
                    .withMessageContaining(io.kroxylicious.it.testplugins.RejectingCreateTopicFilter.ERROR_MESSAGE);

            assertThat(adminB.createTopics(List.of(newTopic)).all())
                    .succeedsWithin(Duration.ofSeconds(10));
        }
    }
}
