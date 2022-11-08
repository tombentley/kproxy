/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.message.ApiVersionsRequestData;
import org.apache.kafka.common.message.MetadataRequestData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.message.SaslAuthenticateRequestData;
import org.apache.kafka.common.message.SaslHandshakeRequestData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.ApiMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import io.kroxylicious.proxy.filter.KrpcFilter;
import io.kroxylicious.proxy.filter.NetFilter;
import io.kroxylicious.proxy.frame.DecodedRequestFrame;
import io.kroxylicious.proxy.internal.KafkaProxyFrontendHandler.State;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol;
import io.netty.handler.ssl.SniCompletionEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class KafkaProxyFrontendHandlerTest {

    public static final String SNI_HOSTNAME = "external.example.com";
    public static final String CLUSTER_HOST = "internal.example.org";
    public static final int CLUSTER_PORT = 9092;
    EmbeddedChannel inboundChannel;
    EmbeddedChannel outboundChannel;

    int corrId = 0;

    private void writeRequest(short apiVersion, ApiMessage body) {
        var apiKey = ApiKeys.forId(body.apiKey());

        int downstreamCorrelationId = corrId++;

        short headerVersion = apiKey.requestHeaderVersion(apiVersion);
        RequestHeaderData header = new RequestHeaderData()
                .setRequestApiKey(apiKey.id)
                .setRequestApiVersion(apiVersion)
                .setClientId("client-id")
                .setCorrelationId(downstreamCorrelationId);

        inboundChannel.writeInbound(new DecodedRequestFrame<>(apiVersion, corrId, true, header, body));
    }

    @BeforeEach
    public void buildChannel() {
        inboundChannel = new EmbeddedChannel();
        corrId = 0;
    }

    @AfterEach
    public void closeChannel() {
        inboundChannel.close();
    }

    public static List<Arguments> provideArgsForExpectedFlow() {
        var result = new ArrayList<Arguments>();
        boolean[] tf = { true, false };
        for (boolean sslConfigured : tf) {
            for (boolean haProxyConfigured : tf) {
                for (boolean saslOffloadConfigured : tf) {
                    for (boolean sendApiVersions : tf) {
                        for (boolean sendSasl : tf) {
                            result.add(Arguments.of(sslConfigured, haProxyConfigured, saslOffloadConfigured, sendApiVersions, sendSasl));
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Test the normal flow, in a number of configurations.
     * @param sslConfigured Whether SSL is configured
     * @param haProxyConfigured
     * @param saslOffloadConfigured
     * @param sendApiVersions
     * @param sendSasl
     */
    @ParameterizedTest
    @MethodSource("provideArgsForExpectedFlow")
    public void expectedFlow(boolean sslConfigured,
                             boolean haProxyConfigured,
                             boolean saslOffloadConfigured,
                             boolean sendApiVersions,
                             boolean sendSasl) {

        var dp = new SaslDecodePredicate(saslOffloadConfigured);
        ArgumentCaptor<NetFilter.NetFilterContext> valueCapture = ArgumentCaptor.forClass(NetFilter.NetFilterContext.class);
        var filter = mock(NetFilter.class);
        doAnswer(i -> {
            NetFilter.NetFilterContext ctx = i.getArgument(0);
            assertEquals(sslConfigured ? SNI_HOSTNAME : null, ctx.sniHostname());
            if (haProxyConfigured) {
                assertEquals("embedded", String.valueOf(ctx.srcAddress()));
                assertEquals("1.2.3.4", ctx.clientAddress());
            }
            else {
                assertEquals("embedded", String.valueOf(ctx.srcAddress()));
                assertEquals("embedded", ctx.clientAddress());
            }
            assertEquals(sendApiVersions ? "foo" : null, ctx.clientSoftwareName());
            assertEquals(sendApiVersions ? "1.0.0" : null, ctx.clientSoftwareVersion());
            if (saslOffloadConfigured && sendSasl) {
                assertEquals("alice", ctx.authorizedId());
            }
            else {
                assertNull(ctx.authorizedId());
            }

            ctx.connect(CLUSTER_HOST, CLUSTER_PORT, new KrpcFilter[0]);
            return null;
        }).when(filter).upstreamBroker(valueCapture.capture());

        var handler = new KafkaProxyFrontendHandler(filter, dp, false, false) {
            @Override
            ChannelFuture getConnect(String remoteHost, int remotePort, Bootstrap b) {
                // This is ugly... basically the EmbeddedChannel doesn't seem to handle the case
                // of a handler creating an outgoing connection and ends up
                // trying to re-register the outbound channel => IllegalStateException
                // So we override this method to short-circuit that
                outboundChannel = new EmbeddedChannel();
                return new DefaultChannelPromise(outboundChannel).setSuccess();
            }
        };
        inboundChannel.pipeline().addLast(handler);
        inboundChannel.pipeline().fireChannelActive();

        assertEquals(State.START, handler.state());

        if (sslConfigured) {
            // Simulate the SSL handler
            inboundChannel.pipeline().fireUserEventTriggered(new SniCompletionEvent(SNI_HOSTNAME));
        }

        if (haProxyConfigured) {
            // Simulate the HA proxy handler
            inboundChannel.writeInbound(new HAProxyMessage(HAProxyProtocolVersion.V1,
                    HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP4,
                    "1.2.3.4", "5.6.7.8", 65535, CLUSTER_PORT));
        }

        if (sendApiVersions) {
            // Simulate the client doing ApiVersions
            writeRequest(ApiVersionsRequestData.HIGHEST_SUPPORTED_VERSION, new ApiVersionsRequestData()
                    .setClientSoftwareName("foo").setClientSoftwareVersion("1.0.0"));
            assertEquals(State.API_VERSIONS, handler.state());
            verify(filter, never()).upstreamBroker(handler);
        }

        if (sendSasl) {
            if (saslOffloadConfigured) {

                // Simulate the KafkaAuthnHandler having done SASL offload
                inboundChannel.pipeline().fireUserEventTriggered(new AuthenticationEvent("alice", Map.of()));

                // If the pipeline is configured for SASL offload (KafkaAuthnHandler)
                // then we assume it DOESN'T propagate the SASL frames down the pipeline
                // and therefore no backend connection happens
                verify(filter, never()).upstreamBroker(handler);
            }
            else {
                // Simulate the client doing SaslHandshake and SaslAuthentication,
                writeRequest(SaslHandshakeRequestData.HIGHEST_SUPPORTED_VERSION, new SaslHandshakeRequestData());

                // these should cause connection to the backend cluster
                handleConnect(filter, handler);
                writeRequest(SaslAuthenticateRequestData.HIGHEST_SUPPORTED_VERSION, new SaslAuthenticateRequestData());
            }
        }

        // Simulate a Metadata request
        writeRequest(MetadataRequestData.HIGHEST_SUPPORTED_VERSION, new MetadataRequestData());
        if (sendSasl && saslOffloadConfigured) {
            handleConnect(filter, handler);
        }

    }

    private void handleConnect(NetFilter filter, KafkaProxyFrontendHandler handler) {
        verify(filter).upstreamBroker(handler);
        assertEquals(State.CONNECTED, handler.state());
        assertFalse(inboundChannel.config().isAutoRead(),
                "Expect inbound autoRead=true, since outbound not yet active");

        // Simular the backend handler receiving channel active and telling the frontent handler
        ChannelHandlerContext outboundContext = outboundChannel.pipeline().context(outboundChannel.pipeline().names().get(0));
        handler.outboundChannelActive(outboundContext);
        outboundChannel.pipeline().fireChannelActive();
        assertTrue(inboundChannel.config().isAutoRead(),
                "Expect inbound autoRead=true, since outbound now active");
    }
}
