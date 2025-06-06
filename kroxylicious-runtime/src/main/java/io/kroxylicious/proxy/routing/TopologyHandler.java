/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import io.kroxylicious.proxy.config.ClusterDefinition;
import io.kroxylicious.proxy.config.RouterDefinition;
import io.kroxylicious.proxy.config.VirtualCluster;
import io.kroxylicious.proxy.filter.FilterAndInvoker;
import io.kroxylicious.proxy.filter.RequestFilterResult;
import io.kroxylicious.proxy.frame.DecodedRequestFrame;
import io.kroxylicious.proxy.frame.OpaqueRequestFrame;
import io.kroxylicious.proxy.internal.InternalCompletionStage;
import io.kroxylicious.proxy.internal.InternalRequestFrame;
import io.kroxylicious.proxy.router.Router;
import io.kroxylicious.proxy.router.RoutingResult;

import edu.umd.cs.findbugs.annotations.NonNull;

public class TopologyHandler extends ChannelDuplexHandler {

    private final VirtualCluster virtualCluster;
    private final Map<String, InitializedRouterFactory<?>> routerFactoriesByName;
    private final Map<String, Router> routers = new HashMap<>();
    private final Map<String, InitializedFilterFactory<?>> filterFactoriesByName;
    private final Router rootRouter;
    private final Map<String, ClusterDefinition> clusterDefinitions;
    private final Map<String, ServerHandler> clusterHandlers;

    public TopologyHandler(
            VirtualCluster virtualCluster,
            Map<String, InitializedRouterFactory<?>> routerFactoriesByName,
            Map<String, InitializedFilterFactory<?>> filterFactoriesByName,
            Map<String, ClusterDefinition> clusterDefinitions) {
        this.virtualCluster = virtualCluster;
        this.routerFactoriesByName = routerFactoriesByName;
        this.filterFactoriesByName = filterFactoriesByName;
        this.rootRouter = getRouter(Objects.requireNonNull(virtualCluster.routerName()));
        this.clusterDefinitions = clusterDefinitions;
        this.clusterHandlers = new HashMap<>();
    }

    @NonNull
    private Router getRouter(String routerName) {
        return this.routers.computeIfAbsent(routerName, k -> {
            return routerFactoriesByName.get(routerName).create();
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof InternalRequestFrame internalRequestFrame) {
            // TODO need to generlise the notion of recipient??
        }
        else if (msg instanceof DecodedRequestFrame<?> frame) {
            // TODO So how do we decode about decodability??
            handleFrame(frame);
        }
        else if (msg instanceof OpaqueRequestFrame || msg == Unpooled.EMPTY_BUFFER) {

        }
        else {
            throw new UnsupportedOperationException(msg.getClass().getName());
        }
    }

    private void handleFrame(DecodedRequestFrame<?> frame) {
        String routerName = virtualCluster.routerName();
        Router router = rootRouter;

        ROUTER:
        while (true) {

            CompletionStage<RoutingResult> routingResultStage = router.onClientRequest(frame.apiVersion(), frame.apiKey(), frame.header(), frame.body(), null);
            var fut0 = routingResultStage.toCompletableFuture();
            if (fut0.isDone()) {
                var rr = fut0.join();
                if (rr instanceof RoutingResultBuilderImpl.ForwardRoutingResult frr) {
                    String routeName = frr.route();
                    InitializedRouterFactory<?> initializedRouterFactory = routerFactoriesByName.get(routerName);
                    RouterDefinition routerDefinition = initializedRouterFactory.routerDefinition();
                    var route = initializedRouterFactory.route(routeName);
                    for (String filterName : route.filterNames()) {
                        FilterAndInvoker filterAndInvoker = null; // look up from member
                        // TODO but need instantiate each filter uniquely
                        extracted(filterAndInvoker);
                    }

                    if (route.routerName() != null) {
                        routerName = route.routerName();
                        router = getRouter(routerName);
                        break ROUTER;
                    }
                    else if (route.clusterName() != null) {
                        ServerHandler handler = this.clusterHandlers.get(route.clusterName());
                        if (handler == null) {
                            var clusterDefinition = this.clusterDefinitions.get(route.clusterName());
                            handler = connectToCluster(clusterDefinition);
                            this.clusterDefinitions.put(route.clusterName(), clusterDefinition);
                        }
                        final Channel outboundChannel = handler.serverCtx.channel();
                        if (outboundChannel.isWritable()) {
                            outboundChannel.write(frame, handler.serverCtx.voidPromise());
                            handler.pendingServerFlushes = true;
                        }
                        else {
                            outboundChannel.writeAndFlush(frame, handler.serverCtx.voidPromise());
                            handler.pendingServerFlushes = false;
                        }
                    }
                    else {
                        throw new IllegalStateException();
                    }
                }
                else {
                    // TODO handle other routing results
                }
            }
            else {
                // TODO handle futures which are not complete
            }
        }
    }

    private ServerHandler connectToCluster(ClusterDefinition clusterDefinition) {
        return null;
    }

    private static void extracted(FilterAndInvoker filterAndInvoker) {
        CompletionStage<RequestFilterResult> filterResultStage = filterAndInvoker.invoker().onRequest(0, null, null, null, null);
        var fut = filterResultStage instanceof InternalCompletionStage
                ? ((InternalCompletionStage<RequestFilterResult>) filterResultStage).getUnderlyingCompletableFuture()
                : filterResultStage.toCompletableFuture();
        if (fut.isDone()) {

        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }
}
