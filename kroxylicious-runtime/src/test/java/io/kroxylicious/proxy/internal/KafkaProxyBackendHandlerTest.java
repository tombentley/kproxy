/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.internal;

import java.util.Optional;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

import io.kroxylicious.proxy.config.TargetCluster;
import io.kroxylicious.proxy.internal.clusternetworkaddressconfigprovider.PortPerBrokerClusterNetworkAddressConfigProvider;
import io.kroxylicious.proxy.model.VirtualCluster;
import io.kroxylicious.proxy.service.HostPort;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaProxyBackendHandlerTest {

    public static final PortPerBrokerClusterNetworkAddressConfigProvider ADDRESS_CONFIG_PROVIDER = new PortPerBrokerClusterNetworkAddressConfigProvider(
            new PortPerBrokerClusterNetworkAddressConfigProvider.PortPerBrokerClusterNetworkAddressConfigProviderConfig(new HostPort("localhost", 9090), "broker-", 9190,
                    0, 10));
    @Mock
    StateHolder stateHolder;

    private KafkaProxyBackendHandler kafkaProxyBackendHandler;
    private ChannelHandlerContext outboundContext;

    @BeforeEach
    void setUp() {
        Channel inboundChannel = new EmbeddedChannel();
        inboundChannel.pipeline().addFirst("dummy", new ChannelDuplexHandler());
        Channel outboundChannel = new EmbeddedChannel();
        outboundChannel.pipeline().addFirst("dummy", new ChannelDuplexHandler());
        kafkaProxyBackendHandler = new KafkaProxyBackendHandler(stateHolder, new VirtualCluster("wibble", new TargetCluster("localhost:9090", Optional.empty()),
                ADDRESS_CONFIG_PROVIDER, Optional.empty(), false, false));
        outboundContext = outboundChannel.pipeline().firstContext();
    }

    @Test
    void shouldNotifyServerActiveOnPlainConnection() throws Exception {
        // Given

        // When
        kafkaProxyBackendHandler.channelActive(outboundContext);

        // Then
        verify(stateHolder).onServerActive();
    }

    @Test
    void shouldNotifyServerExceptionOnExceptionCaught() {
        // Given
        RuntimeException kaboom = new RuntimeException("Kaboom");

        // When
        kafkaProxyBackendHandler.exceptionCaught(outboundContext, kaboom);

        // Then
        verify(stateHolder).onServerException(kaboom);
    }

    @Test
    void shouldNotifyServerActiveOnTlsNegotiated() throws Exception {
        // Given
        var serverCtx = mock(ChannelHandlerContext.class);

        // When
        kafkaProxyBackendHandler.userEventTriggered(serverCtx, SslHandshakeCompletionEvent.SUCCESS);

        // Then
        verify(stateHolder).onServerActive();
    }

    @Test
    void shouldNotifyServerActiveOnFailedTls() throws Exception {
        // Given
        var serverCtx = mock(ChannelHandlerContext.class);
        SSLHandshakeException cause = new SSLHandshakeException("Oops!");

        // When
        kafkaProxyBackendHandler.userEventTriggered(serverCtx, new SslHandshakeCompletionEvent(cause));

        // Then
        verify(stateHolder).onServerException(cause);
    }
}
