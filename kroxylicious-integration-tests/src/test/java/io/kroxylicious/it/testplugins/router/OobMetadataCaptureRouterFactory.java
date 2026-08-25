/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.it.testplugins.router;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.kafka.common.message.MetadataRequestData;
import org.apache.kafka.common.message.MetadataResponseData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.protocol.ApiKeys;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.proxy.router.Router;
import io.kroxylicious.proxy.router.RouterFactory;
import io.kroxylicious.proxy.router.RouterFactoryContext;

/**
 * A test router that, for each inbound request, first sends an out-of-band METADATA
 * request to {@link Config#route()}, captures the broker node IDs from the response,
 * then forwards the original request to the same route. Used in tests to verify that
 * node-ID translation is applied exactly once when an outer router dispatches OOB
 * requests through a nested router.
 */
@Plugin(configType = OobMetadataCaptureRouterFactory.Config.class)
public class OobMetadataCaptureRouterFactory
        implements RouterFactory<OobMetadataCaptureRouterFactory.Config, OobMetadataCaptureRouterFactory.Config> {

    public record Config(String route) {}

    private static final AtomicReference<List<Integer>> capturedBrokerIds = new AtomicReference<>();

    public static void reset() {
        capturedBrokerIds.set(null);
    }

    public static List<Integer> capturedBrokerIds() {
        return capturedBrokerIds.get();
    }

    @Override
    public Config initialize(RouterFactoryContext context, Config config) {
        return config;
    }

    @Override
    public Router createRouter(RouterFactoryContext context, Config config) {
        return (apiKey, apiVersion, header, request, ctx) -> {
            var metadataRequest = new MetadataRequestData();
            var metadataHeader = new RequestHeaderData()
                    .setRequestApiKey(ApiKeys.METADATA.id)
                    .setRequestApiVersion(metadataRequest.highestSupportedVersion())
                    .setClientId(header.clientId());
            return ctx.sendRequest(ctx.anyNode(config.route()), metadataHeader, metadataRequest)
                    .thenCompose(metadataBody -> {
                        var md = (MetadataResponseData) metadataBody;
                        capturedBrokerIds.set(md.brokers().stream()
                                .map(MetadataResponseData.MetadataResponseBroker::nodeId)
                                .collect(Collectors.toList()));
                        return ctx.sendRequest(ctx.anyNode(config.route()), header, request);
                    })
                    .thenCompose(body -> ctx.respondWith(body).completed());
        };
    }
}
