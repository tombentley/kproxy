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
import io.kroxylicious.proxy.internal.codec.CorrelationManager;
import io.kroxylicious.proxy.internal.codec.DecodePredicate;
import io.kroxylicious.proxy.internal.codec.KafkaRequestEncoder;
import io.kroxylicious.proxy.internal.codec.KafkaResponseDecoder;
import io.kroxylicious.proxy.tag.VisibleForTesting;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SniCompletionEvent;

public class KafkaProxyFrontendHandler
        extends ChannelInboundHandlerAdapter
        implements NetFilter.NetFilterContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProxyFrontendHandler.class);

    /** Cache ApiVersions response which we use when returning ApiVersions ourselves */
    private static final ApiVersionsResponseData API_VERSIONS_RESPONSE;
    static {
        var objectMapper = new ObjectMapper();
        try (var parser = KafkaProxyFrontendHandler.class.getResourceAsStream("/ApiVersions-3.2.json")) {
            API_VERSIONS_RESPONSE = ApiVersionsResponseDataJsonConverter.read(objectMapper.readTree(parser), (short) 3);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private final boolean logNetwork;
    private final boolean logFrames;

    private ChannelHandlerContext outboundCtx;
    private KafkaProxyBackendHandler backendHandler;
    private boolean pendingFlushes;

    private final NetFilter filter;
    private final SaslDecodePredicate dp;

    private AuthenticationEvent authentication;

    private String clientSoftwareName;
    private String clientSoftwareVersion;
    private String sniHostname;

    private ChannelHandlerContext inboundCtx;
    // The message buffered while we connect to the outbound cluster
    // There can only be one such because auto read is disabled until outbound
    // channel activation
    private Object bufferedMsg;
    // Flag if we receive a channelReadComplete() prior to outbound connection activation
    // so we can perform the channelReadComplete()/outbound flush & auto_read
    // once the outbound channel is active
    private boolean pendingReadComplete = true;

    @VisibleForTesting
    enum State {
        /** The initial state */
        START,
        /** An HAProxy message has been received */
        HA_PROXY,
        /** A Kafka ApiVersions request has been received */
        API_VERSIONS,
        /** Some other Kafka request has been received and we're in the process of connecting to the outbound cluster */
        CONNECTING,
        /** The outbound connection is connected but not yet active */
        CONNECTED,
        /** The outbound connection is active */
        OUTBOUND_ACTIVE,
        /** The connection to the outbound cluster failed */
        FAILED
    }

    /**
     * The current state.
     * Transitions:
     * <code><pre>
     * ST_START ──→ ST_HA_PROXY ──→ ST_API_VERSIONS ─╭─→ ST_CONNECTING ──→ ST_CONNECTED
     *     ╰─────────────╰────────────────╰──────────╯      ╰──→ ST_FAILED
     * </pre></code>
     * Unexpected state transitions and exceptions also cause a
     * transition to {@link State#FAILED} (via {@link #illegalState(String)}}
     */
    private State state = State.START;

    private boolean isInboundBlocked = true;
    private HAProxyMessage haProxyMessage;

    KafkaProxyFrontendHandler(NetFilter filter,
                              SaslDecodePredicate dp,
                              boolean logNetwork,
                              boolean logFrames) {
        this.filter = filter;
        this.dp = dp;
        this.logNetwork = logNetwork;
        this.logFrames = logFrames;
    }

    private IllegalStateException illegalState(String msg) {
        String name = state.name();
        state = State.FAILED;
        return new IllegalStateException((msg == null ? "" : msg + ", ") + "state=" + name);
    }

    @VisibleForTesting
    State state() {
        return state;
    }

    public void outboundChannelActive(ChannelHandlerContext ctx) {
        if (state != State.CONNECTED) {
            throw illegalState(null);
        }
        LOGGER.trace("{}: outboundChannelActive", inboundCtx.channel().id());
        outboundCtx = ctx;
        // connection is complete, so first forward the buffered message
        forwardOutbound(ctx, bufferedMsg);
        bufferedMsg = null; // don't pin in memory once we no longer need it
        if (pendingReadComplete) {
            pendingReadComplete = false;
            channelReadComplete(ctx);
        }
        state = State.OUTBOUND_ACTIVE;

        var inboundChannel = this.inboundCtx.channel();
        // once buffered message has been forwarded we enable auto-read to start accepting further messages
        inboundChannel.config().setAutoRead(true);
    }

    @Override
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        // this is key to propagate back-pressure changes
        if (backendHandler != null) {
            backendHandler.inboundChannelWritabilityChanged(ctx);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (state == State.OUTBOUND_ACTIVE) { // post-backend connection
            forwardOutbound(ctx, msg);
        }
        else { // pre-backend connection
            if (state == State.START
                    && msg instanceof HAProxyMessage) {
                this.haProxyMessage = (HAProxyMessage) msg;
                state = State.HA_PROXY;
            }
            else if ((state == State.START
                    || state == State.HA_PROXY)
                    && msg instanceof DecodedRequestFrame
                    && ((DecodedRequestFrame<?>) msg).apiKey() == ApiKeys.API_VERSIONS) {
                // This handler can respond to ApiVersions itself
                writeApiVersionsResponse(ctx, (DecodedRequestFrame<ApiVersionsRequestData>) msg);
                // ctx.fireChannelRead(msg);
                // Request to read the following request
                ctx.channel().read();
                state = State.API_VERSIONS;
            }
            else if ((state == State.START
                    || state == State.HA_PROXY
                    || state == State.API_VERSIONS)
                    && msg instanceof RequestFrame) {
                if (bufferedMsg != null) {
                    // Single buffered message assertion failed
                    throw illegalState("Already have buffered msg");
                }
                state = State.CONNECTING;
                // But for any other request we'll need a backend connection
                // (for which we need to ask the filter which cluster to connect to
                // and with what filters)
                this.bufferedMsg = msg;
                // TODO ensure that the filter makes exactly one upstream connection?
                // Or not for the topic routing case

                // Note filter.upstreamBroker will call back on the connect() method below
                filter.upstreamBroker(this);
            }
            else {
                throw illegalState("Unexpected channelRead() message of " + msg.getClass());
            }
        }
    }

    @Override
    public void connect(String remoteHost, int remotePort, KrpcFilter[] filters) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{}: Connecting to backend broker {}:{} using filters {}",
                    inboundCtx.channel().id(), remoteHost, remotePort, Arrays.toString(filters));
        }
        var correlationManager = new CorrelationManager();

        final Channel inboundChannel = inboundCtx.channel();

        // Start the upstream connection attempt.
        Bootstrap b = new Bootstrap();
        backendHandler = new KafkaProxyBackendHandler(this, inboundCtx);
        b.group(inboundChannel.eventLoop())
                .channel(inboundChannel.getClass())
                .handler(backendHandler)
                .option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.TCP_NODELAY, true);

        LOGGER.trace("Connecting to outbound {}:{}", remoteHost, remotePort);
        ChannelFuture connectFuture = getConnect(remoteHost, remotePort, b);
        Channel outboundChannel = connectFuture.channel();
        ChannelPipeline pipeline = outboundChannel.pipeline();

        if (logFrames) {
            pipeline.addFirst("frameLogger", new LoggingHandler("io.kroxylicious.proxy.internal.UpstreamFrameLogger"));
        }
        addFiltersToPipeline(filters, pipeline);
        pipeline.addFirst("responseDecoder", new KafkaResponseDecoder(correlationManager));
        pipeline.addFirst("requestEncoder", new KafkaRequestEncoder(correlationManager));
        if (logNetwork) {
            pipeline.addFirst("networkLogger", new LoggingHandler("io.kroxylicious.proxy.internal.UpstreamNetworkLogger"));
        }

        connectFuture.addListener(future -> {
            if (future.isSuccess()) {
                state = State.CONNECTED;
                LOGGER.trace("{}: Outbound connected", inboundCtx.channel().id());
                // Now we know which filters are to be used we need to update the DecodePredicate
                // so that the decoder starts decoding the messages that the filters want to intercept
                dp.setDelegate(DecodePredicate.forFilters(filters));
            }
            else {
                state = State.FAILED;
                // Close the connection if the connection attempt has failed.
                LOGGER.trace("Outbound connect error, closing inbound channel", future.cause());
                inboundChannel.close();
            }
        });
    }

    @VisibleForTesting
    ChannelFuture getConnect(String remoteHost, int remotePort, Bootstrap b) {
        return b.connect(remoteHost, remotePort);
    }

    private void addFiltersToPipeline(KrpcFilter[] filters, ChannelPipeline pipeline) {
        for (var filter : filters) {
            pipeline.addFirst(filter.toString(), new FilterHandler(filter, 20000));
        }
    }

    public void forwardOutbound(final ChannelHandlerContext ctx, Object msg) {
        if (outboundCtx == null) {
            LOGGER.trace("READ on inbound {} ignored because outbound is not active (msg: {})",
                    ctx.channel(), msg);
            return;
        }
        final Channel outboundChannel = outboundCtx.channel();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("READ on inbound {} outbound {} (outbound.isWritable: {}, msg: {})",
                    ctx.channel(), outboundChannel, outboundChannel.isWritable(), msg);
            LOGGER.trace("Outbound bytesBeforeUnwritable: {}", outboundChannel.bytesBeforeUnwritable());
            LOGGER.trace("Outbound config: {}", outboundChannel.config());
            LOGGER.trace("Outbound is active, writing and flushing {}", msg);
        }
        if (outboundChannel.isWritable()) {
            outboundChannel.write(msg, outboundCtx.voidPromise());
            pendingFlushes = true;
        }
        else {
            outboundChannel.writeAndFlush(msg, outboundCtx.voidPromise());
            pendingFlushes = false;
        }
        LOGGER.trace("/READ");
    }

    /**
     * Sends an ApiVersions response from this handler to the client
     * (i.e. prior to having backend connection)
     */
    private void writeApiVersionsResponse(ChannelHandlerContext ctx, DecodedRequestFrame<ApiVersionsRequestData> frame) {
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
    }

    public void outboundWritabilityChanged(ChannelHandlerContext outboundCtx) {
        if (this.outboundCtx != outboundCtx) {
            throw illegalState("Mismatching outboundCtx");
        }
        if (isInboundBlocked && outboundCtx.channel().isWritable()) {
            isInboundBlocked = false;
            inboundCtx.channel().config().setAutoRead(true);
        }
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
        if (outboundCtx == null) {
            LOGGER.trace("READ_COMPLETE on inbound {}, ignored because outbound is not active",
                    ctx.channel());
            pendingReadComplete = true;
            return;
        }
        final Channel outboundChannel = outboundCtx.channel();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("READ_COMPLETE on inbound {} outbound {} (pendingFlushes: {}, isInboundBlocked: {}, output.isWritable: {})",
                    ctx.channel(), outboundChannel,
                    pendingFlushes, isInboundBlocked, outboundChannel.isWritable());
        }
        if (pendingFlushes) {
            pendingFlushes = false;
            outboundChannel.flush();
        }
        if (!outboundChannel.isWritable()) {
            ctx.channel().config().setAutoRead(false);
            isInboundBlocked = true;
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.trace("INACTIVE on inbound {}", ctx.channel());
        if (outboundCtx == null) {
            return;
        }
        final Channel outboundChannel = outboundCtx.channel();
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
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
        else if (event instanceof AuthenticationEvent) {
            this.authentication = (AuthenticationEvent) event;
        }
        super.userEventTriggered(ctx, event);
    }

    @Override
    public String clientAddress() {
        if (haProxyMessage != null) {
            return haProxyMessage.sourceAddress();
        }
        else {
            SocketAddress socketAddress = inboundCtx.channel().remoteAddress();
            if (socketAddress instanceof InetSocketAddress) {
                return ((InetSocketAddress) socketAddress).getAddress().getHostAddress();
            }
            else {
                return String.valueOf(socketAddress);
            }
        }
    }

    @Override
    public int clientPort() {
        if (haProxyMessage != null) {
            return haProxyMessage.sourcePort();
        }
        else {
            SocketAddress socketAddress = inboundCtx.channel().remoteAddress();
            if (socketAddress instanceof InetSocketAddress) {
                return ((InetSocketAddress) socketAddress).getPort();
            }
            else {
                return -1;
            }
        }
    }

    @Override
    public SocketAddress srcAddress() {
        return inboundCtx.channel().remoteAddress();
    }

    @Override
    public String authorizedId() {
        return authentication != null ? authentication.authorizationId() : null;
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
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.inboundCtx = ctx;
        LOGGER.trace("{}: channelActive", inboundCtx.channel().id());
        // Initially the channel is not auto reading, so read the first batch of requests
        ctx.channel().config().setAutoRead(false);
        ctx.channel().read();
        super.channelActive(ctx);
    }

    @Override
    public String toString() {
        return "KafkaProxyFrontendHandler{inbound = " + inboundCtx.channel() + ", state = " + state + "}";
    }
}
