/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;

import org.apache.kafka.common.message.ApiVersionsRequestData;
import org.apache.kafka.common.message.ApiVersionsResponseData;
import org.apache.kafka.common.message.ApiVersionsResponseDataJsonConverter;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.proxy.filter.KrpcFilter;
import io.kroxylicious.proxy.filter.NetFilter;
import io.kroxylicious.proxy.frame.DecodedRequestFrame;
import io.kroxylicious.proxy.frame.DecodedResponseFrame;
import io.kroxylicious.proxy.frame.RequestFrame;
import io.kroxylicious.proxy.internal.codec.DecodePredicate;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SniCompletionEvent;

/**
 * A ChannelInboundHandlerAdapter providing the {@link NetFilter} abstraction
 * for determining which upstream cluster to connect to.
 */
public class NetHandler extends ChannelInboundHandlerAdapter implements NetFilter.NetFilterContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetHandler.class);

    private static final ApiVersionsResponseData API_VERSIONS_RESPONSE;

    static {
        var objectMapper = new ObjectMapper();
        try (var parser = NetHandler.class.getResourceAsStream("/ApiVersions-3.2.json")) {
            API_VERSIONS_RESPONSE = ApiVersionsResponseDataJsonConverter.read(objectMapper.readTree(parser), (short) 3);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private final NetFilter filter;
    private final boolean logNetwork;
    private final boolean logFrames;
    private final MyDecodePredicate dp;

    private AuthenticationEvent authentication;

    private String clientSoftwareName;
    private String clientSoftwareVersion;
    private String sniHostname;
    private SocketAddress clientAddress;
    private ChannelHandlerContext ctx;

    NetHandler(NetFilter filter, MyDecodePredicate dp, boolean logNetwork, boolean logFrames) {
        this.filter = filter;
        this.dp = dp;
        this.logNetwork = logNetwork;
        this.logFrames = logFrames;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LOGGER.debug("{}: Read request: {}", ctx.channel(), msg);
        if (msg instanceof DecodedRequestFrame
                && ((DecodedRequestFrame<?>) msg).apiKey() == ApiKeys.API_VERSIONS) {
            onApiVersions(ctx, (DecodedRequestFrame<ApiVersionsRequestData>) msg);
            ctx.fireChannelRead(msg);
        }
        else if (msg instanceof RequestFrame) {
            this.ctx = ctx;
            // TODO ensure that the filter makes exactly one upstream connection?
            // Or not for the topic routing case
            // Note filter.upstreamBroker will call back on the connect() method below
            filter.upstreamBroker(this);

            // Forward the request now that the handler chain has a frontend handler
            LOGGER.debug("{}: Forwarding request: {}", ctx.channel(), msg);

            ctx.fireChannelRead(msg);
            ctx.pipeline().remove(this);
        }
        else {
            throw new IllegalStateException("Unexpected read event message " + msg.getClass());
        }
    }

    @Override
    public void connect(String host, int port, KrpcFilter[] filters) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{}: Connecting to backend broker {}:{} using filters {}", ctx.channel(), host, port, Arrays.toString(filters));
        }

        var frontendHandler = new KafkaProxyFrontendHandler(
                host,
                port,
                filters,
                logNetwork,
                logFrames);

        // Now we know which filters are being used we need to update the DecodePredicate
        // so that the decoder starts decoding the messages that the filters want to intercept
        dp.setDelegate(DecodePredicate.forFilters(filters));

        // replace this handler in the frontend pipeline
        ChannelPipeline pipeline = ctx.pipeline();
        // pipeline.replace(this, "frontendHandler", frontendHandler);
        pipeline.addLast(frontendHandler);
        LOGGER.debug("{}: Revised pipeline: {}", ctx.channel(), pipeline);

        // TODO how does this handler get activated (with its own context)
        // try {
        // frontendHandler.channelRegistered(???);
        // } catch (Exception e) {
        // throw new RuntimeException(e);
        // }
        // frontendHandler.channelActive(???);
    }

    private void onApiVersions(ChannelHandlerContext ctx, DecodedRequestFrame<ApiVersionsRequestData> frame) {
        // TODO check the format of the strings using a regex
        this.clientSoftwareName = frame.body().clientSoftwareName();
        this.clientSoftwareVersion = frame.body().clientSoftwareVersion();

        short apiVersion = frame.apiVersion();
        int correlationId = frame.correlationId();
        ResponseHeaderData header = new ResponseHeaderData()
                .setCorrelationId(correlationId);
        LOGGER.debug("{}: Writing ApiVersions response", ctx.channel());
        ctx.writeAndFlush(new DecodedResponseFrame<>(
                apiVersion, correlationId, header, API_VERSIONS_RESPONSE));
        ctx.channel().read();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (event instanceof SniCompletionEvent) {
            SniCompletionEvent sniCompletionEvent = (SniCompletionEvent) event;
            if (sniCompletionEvent.isSuccess()) {
                this.sniHostname = sniCompletionEvent.hostname();
            }
            // TODO handle the failure case
        }
        else if (event instanceof PROXYDecodeEvent) {
            var proxyDecodeEvent = (PROXYDecodeEvent) event;
            this.clientAddress = new InetSocketAddress(proxyDecodeEvent.srcAddr(), proxyDecodeEvent.srcPort());
        }
        else if (event instanceof AuthenticationEvent) {
            this.authentication = (AuthenticationEvent) event;
        }
        super.userEventTriggered(ctx, event);
    }

    @Override
    public SocketAddress clientAddress() {
        return clientAddress;
    }

    @Override
    public SocketAddress srcAddress() {
        return ctx.channel().remoteAddress();
    }

    @Override
    public String authorizedId() {
        return authentication.authorizationId();
    }

    @Override
    public String clientSoftwareName() {
        return clientSoftwareName;
    }

    @Override
    public String clientSoftwareVersion() {
        return clientSoftwareVersion;
    }

    @Override
    public String sniHostname() {
        return sniHostname;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Oops", cause);
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("channelRegistered");
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("channelUnregistered");
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("channelActive");
        ctx.channel().read();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("channelInactive");
        super.channelInactive(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("channelReadComplete");
        super.channelReadComplete(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("channelWritabilityChanged");
        super.channelWritabilityChanged(ctx);
    }
}
