/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.it;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.common.message.CreateTopicsRequestData;
import org.apache.kafka.common.message.CreateTopicsResponseData;
import org.apache.kafka.common.message.DescribeClusterRequestData;
import org.apache.kafka.common.message.DescribeClusterResponseData;
import org.apache.kafka.common.message.MetadataResponseData;
import org.apache.kafka.common.protocol.Errors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.nettyplus.leakdetector.junit.NettyLeakDetectorExtension;

import io.kroxylicious.it.testplugins.OutOfBandSendFilterFactory;
import io.kroxylicious.it.testplugins.RequestResponseMarkingFilter;
import io.kroxylicious.it.testplugins.RequestResponseMarkingFilterFactory;
import io.kroxylicious.it.testplugins.router.ClientIdRouterFactory;
import io.kroxylicious.it.testplugins.router.DynamicProduceRouterFactory;
import io.kroxylicious.it.testplugins.router.OobMetadataCaptureRouterFactory;
import io.kroxylicious.it.testplugins.router.PassThroughRouterFactory;
import io.kroxylicious.proxy.config.ClusterDefinition;
import io.kroxylicious.proxy.config.RouteDefinition;
import io.kroxylicious.proxy.config.RouteTarget;
import io.kroxylicious.proxy.config.RouterDefinition;
import io.kroxylicious.proxy.config.VirtualClusterBuilder;
import io.kroxylicious.proxy.internal.config.Feature;
import io.kroxylicious.proxy.internal.config.Features;
import io.kroxylicious.testing.integration.Request;
import io.kroxylicious.testing.integration.ResponsePayload;
import io.kroxylicious.testing.integration.config.NamedFilterDefinitionBuilder;
import io.kroxylicious.testing.integration.tester.KroxyliciousTesters;

import static io.kroxylicious.it.testplugins.RequestResponseMarkingFilter.FILTER_NAME_TAG;
import static io.kroxylicious.testing.integration.tester.KroxyliciousConfigUtils.OS_ASSIGNED_BOOTSTRAP;
import static io.kroxylicious.testing.integration.tester.KroxyliciousConfigUtils.baseConfigurationBuilder;
import static io.kroxylicious.testing.integration.tester.KroxyliciousConfigUtils.defaultPortIdentifiesNodeGatewayBuilder;
import static org.apache.kafka.common.protocol.ApiKeys.CREATE_TOPICS;
import static org.apache.kafka.common.protocol.ApiKeys.DESCRIBE_CLUSTER;
import static org.apache.kafka.common.protocol.ApiKeys.METADATA;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a filter on an outer route (targeting a nested router) can
 * send out-of-band (OOB) requests correctly.
 *
 * <p>The bug: the nested {@code RoutingHandler} was routing OOB
 * {@link io.kroxylicious.proxy.frame.InternalRequestFrame}s to
 * {@code handleCompletion} instead of {@code handleNestedOobCompletion},
 * so the OOB promise was never completed and the originating filter hung.
 *
 * <p>Topology:
 * <pre>
 * Client → demo VC
 *   → outer router (PassThrough → "to-inner")
 *   → outer route "to-inner" [OutOfBandSendFilter]
 *   → inner router (DynamicProduce → "backend")
 *   → inner route "backend" [RequestResponseMarkingFilter]
 *   → mock server
 * </pre>
 */
@ExtendWith(NettyLeakDetectorExtension.class)
class NestedRouterOobIT {

    private static final Features ROUTING_ENABLED = Features.builder().enable(Feature.ROUTING).build();

    @Test
    void outerRouteFilterShouldHandleOobRequestViaNestedRouter() {
        // Given
        var outerOobFilter = new NamedFilterDefinitionBuilder("oob-sender", OutOfBandSendFilterFactory.class.getName())
                .withConfig(Map.of("apiKeyToSend", CREATE_TOPICS, "tagToCollect", FILTER_NAME_TAG))
                .build();
        var innerMarkerFilter = new NamedFilterDefinitionBuilder("inner-marker", RequestResponseMarkingFilterFactory.class.getName())
                .withConfig("name", "inner-marker", "keysToMark", Set.of(CREATE_TOPICS))
                .build();

        var innerRoute = new RouteDefinition("backend", 0, List.of("inner-marker"), new RouteTarget("mock-cluster", null));
        var innerRouter = new RouterDefinition("inner",
                DynamicProduceRouterFactory.class.getName(),
                new DynamicProduceRouterFactory.Config("backend"),
                List.of(innerRoute));

        var outerRoute = new RouteDefinition("to-inner", 0, List.of("oob-sender"), new RouteTarget(null, "inner"));
        var outerRouter = new RouterDefinition("outer",
                PassThroughRouterFactory.class.getName(),
                new PassThroughRouterFactory.Config("to-inner"),
                List.of(outerRoute));

        var vc = new VirtualClusterBuilder()
                .withName("demo")
                .withTarget(new RouteTarget(null, "outer"))
                .addToGateways(defaultPortIdentifiesNodeGatewayBuilder(OS_ASSIGNED_BOOTSTRAP).build())
                .build();

        try (var tester = KroxyliciousTesters.mockKafkaKroxyliciousTester(s -> {
            var clusterDef = new ClusterDefinition("mock-cluster", s, null);
            return baseConfigurationBuilder()
                    .addToClusterDefinitions(clusterDef)
                    .addToFilterDefinitions(outerOobFilter, innerMarkerFilter)
                    .addToRouterDefinitions(outerRouter, innerRouter)
                    .addToVirtualClusters(vc);
        }, ROUTING_ENABLED);
                var client = tester.simpleTestClient()) {

            tester.addMockResponseForApiKey(new ResponsePayload(CREATE_TOPICS, CREATE_TOPICS.latestVersion(), arbitraryCreateTopicsResponse()));
            tester.addMockResponseForApiKey(new ResponsePayload(DESCRIBE_CLUSTER, DESCRIBE_CLUSTER.latestVersion(), arbitraryDescribeClusterResponse()));

            // When
            var response = client.getSync(
                    new Request(DESCRIBE_CLUSTER, DESCRIBE_CLUSTER.latestVersion(), "client", new DescribeClusterRequestData()));

            // Then
            var responseData = (DescribeClusterResponseData) response.payload().message();
            assertThat(responseData.errorMessage()).isEqualTo(
                    "filterNameTaggedFieldsFromOutOfBandResponse: "
                            + RequestResponseMarkingFilter.class.getSimpleName() + "-inner-marker-response");
        }
    }

    /**
     * Verifies that the broker node IDs received by an outer router from an OOB METADATA
     * request through a nested router reflect only the nested level's virtual node ID
     * translation — not a second translation applied at the outer level.
     *
     * <p>Topology: outer router (2 routes → BijectiveMapping at outer level) sends OOB
     * METADATA to a nested router (2 routes → BijectiveMapping at inner level) where
     * METADATA is statically routed to "backend-a". The mock cluster returns broker
     * nodeId=1; the inner BijectiveMapping translates that to nested-virtual 2 (routeId=0,
     * S=2: 0 + 2×1 = 2). The outer router should receive nodeId=2.
     */
    @Test
    void outerRouterShouldReceiveCorrectNodeIdsFromOobViaNestedStaticRoute() {
        // Given
        OobMetadataCaptureRouterFactory.reset();

        // Inner router: statically routes everything to "backend-a"; 2 routes force BijectiveMapping
        var innerRouteA = new RouteDefinition("backend-a", 0, List.of(), new RouteTarget("mock-cluster", null));
        var innerRouteB = new RouteDefinition("backend-b", 1, List.of(), new RouteTarget("mock-cluster", null));
        var innerRouter = new RouterDefinition("inner",
                PassThroughRouterFactory.class.getName(),
                new PassThroughRouterFactory.Config("backend-a"),
                List.of(innerRouteA, innerRouteB));

        // Outer router: 2 routes force BijectiveMapping; "to-unused" is never exercised
        var outerRoute = new RouteDefinition("to-nested", 0, List.of(), new RouteTarget(null, "inner"));
        var unusedRoute = new RouteDefinition("to-unused", 1, List.of(), new RouteTarget("mock-cluster", null));
        var outerRouter = new RouterDefinition("outer",
                OobMetadataCaptureRouterFactory.class.getName(),
                new OobMetadataCaptureRouterFactory.Config("to-nested"),
                List.of(outerRoute, unusedRoute));

        var vc = new VirtualClusterBuilder()
                .withName("demo")
                .withTarget(new RouteTarget(null, "outer"))
                .addToGateways(defaultPortIdentifiesNodeGatewayBuilder(OS_ASSIGNED_BOOTSTRAP).build())
                .build();

        try (var tester = KroxyliciousTesters.mockKafkaKroxyliciousTester(s -> {
            var clusterDef = new ClusterDefinition("mock-cluster", s, null);
            return baseConfigurationBuilder()
                    .addToClusterDefinitions(clusterDef)
                    .addToRouterDefinitions(outerRouter, innerRouter)
                    .addToVirtualClusters(vc);
        }, ROUTING_ENABLED);
                var client = tester.simpleTestClient()) {

            tester.addMockResponseForApiKey(new ResponsePayload(METADATA, METADATA.latestVersion(), metadataResponseWithNodeId(1)));
            tester.addMockResponseForApiKey(new ResponsePayload(CREATE_TOPICS, CREATE_TOPICS.latestVersion(), arbitraryCreateTopicsResponse()));

            // When
            client.getSync(new Request(CREATE_TOPICS, CREATE_TOPICS.latestVersion(), "client", new CreateTopicsRequestData()));

            // Then: inner BijectiveMapping(S=2, "backend-a"→id=0): toVirtual("backend-a", 1) = 0 + 2×1 = 2
            assertThat(OobMetadataCaptureRouterFactory.capturedBrokerIds())
                    .as("OOB METADATA broker nodeId should be translated by the inner level only")
                    .containsExactly(2);
        }
    }

    /**
     * Verifies that the broker node IDs received by an outer router from an OOB METADATA
     * request through a nested router reflect only the nested level's virtual node ID
     * translation — not a second translation applied at the outer level.
     *
     * <p>This variant uses a nested router that routes METADATA dynamically (via
     * {@code onRequest}) rather than via a static route, exercising the path where the
     * outer router's OOB frame is dispatched to the nested router's {@code onRequest},
     * which issues its own OOB to the cluster.
     */
    @Test
    void outerRouterShouldReceiveCorrectNodeIdsFromOobViaNestedDynamicRoute() {
        // Given
        OobMetadataCaptureRouterFactory.reset();

        // Inner router: all requests dynamic (ClientIdRouterFactory has no staticRoutes());
        // 2 routes force BijectiveMapping; default route ensures OOB header always resolves to "backend-a"
        var innerRouteA = new RouteDefinition("backend-a", 0, List.of(), new RouteTarget("mock-cluster", null));
        var innerRouteB = new RouteDefinition("backend-b", 1, List.of(), new RouteTarget("mock-cluster", null));
        var innerConfig = new ClientIdRouterFactory.Config(Map.of(), "backend-a");
        var innerRouter = new RouterDefinition("inner",
                ClientIdRouterFactory.class.getName(),
                innerConfig,
                List.of(innerRouteA, innerRouteB));

        // Outer router: 2 routes force BijectiveMapping; "to-unused" is never exercised
        var outerRoute = new RouteDefinition("to-nested", 0, List.of(), new RouteTarget(null, "inner"));
        var unusedRoute = new RouteDefinition("to-unused", 1, List.of(), new RouteTarget("mock-cluster", null));
        var outerRouter = new RouterDefinition("outer",
                OobMetadataCaptureRouterFactory.class.getName(),
                new OobMetadataCaptureRouterFactory.Config("to-nested"),
                List.of(outerRoute, unusedRoute));

        var vc = new VirtualClusterBuilder()
                .withName("demo")
                .withTarget(new RouteTarget(null, "outer"))
                .addToGateways(defaultPortIdentifiesNodeGatewayBuilder(OS_ASSIGNED_BOOTSTRAP).build())
                .build();

        try (var tester = KroxyliciousTesters.mockKafkaKroxyliciousTester(s -> {
            var clusterDef = new ClusterDefinition("mock-cluster", s, null);
            return baseConfigurationBuilder()
                    .addToClusterDefinitions(clusterDef)
                    .addToRouterDefinitions(outerRouter, innerRouter)
                    .addToVirtualClusters(vc);
        }, ROUTING_ENABLED);
                var client = tester.simpleTestClient()) {

            tester.addMockResponseForApiKey(new ResponsePayload(METADATA, METADATA.latestVersion(), metadataResponseWithNodeId(1)));
            tester.addMockResponseForApiKey(new ResponsePayload(CREATE_TOPICS, CREATE_TOPICS.latestVersion(), arbitraryCreateTopicsResponse()));

            // When
            client.getSync(new Request(CREATE_TOPICS, CREATE_TOPICS.latestVersion(), "client", new CreateTopicsRequestData()));

            // Then: inner BijectiveMapping(S=2, "backend-a"→id=0): toVirtual("backend-a", 1) = 0 + 2×1 = 2
            assertThat(OobMetadataCaptureRouterFactory.capturedBrokerIds())
                    .as("OOB METADATA broker nodeId should be translated by the inner level only")
                    .containsExactly(2);
        }
    }

    private static MetadataResponseData metadataResponseWithNodeId(int nodeId) {
        var broker = new MetadataResponseData.MetadataResponseBroker();
        broker.setNodeId(nodeId);
        broker.setHost("mock-host");
        broker.setPort(9092);
        var response = new MetadataResponseData();
        response.brokers().add(broker);
        return response;
    }

    private static CreateTopicsResponseData arbitraryCreateTopicsResponse() {
        var message = new CreateTopicsResponseData();
        var topic = new CreateTopicsResponseData.CreatableTopicResult();
        topic.setName("mockTopic");
        topic.setNumPartitions(3);
        topic.setReplicationFactor((short) 3);
        message.topics().add(topic);
        return message;
    }

    private static DescribeClusterResponseData arbitraryDescribeClusterResponse() {
        var message = new DescribeClusterResponseData();
        message.setErrorMessage("arbitrary");
        message.setErrorCode(Errors.UNSUPPORTED_VERSION.code());
        return message;
    }
}
