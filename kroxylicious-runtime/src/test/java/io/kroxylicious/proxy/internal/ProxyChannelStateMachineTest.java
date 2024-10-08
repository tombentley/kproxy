/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.internal;

import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLException;

import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.UnknownServerException;
import org.apache.kafka.common.message.ApiVersionsRequestData;
import org.apache.kafka.common.message.ApiVersionsResponseData;
import org.apache.kafka.common.message.MetadataRequestData;
import org.apache.kafka.common.message.MetadataResponseData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.protocol.Errors;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol;
import io.netty.handler.ssl.SslContextBuilder;

import io.kroxylicious.proxy.filter.FilterAndInvoker;
import io.kroxylicious.proxy.filter.NetFilter;
import io.kroxylicious.proxy.frame.DecodedRequestFrame;
import io.kroxylicious.proxy.frame.DecodedResponseFrame;
import io.kroxylicious.proxy.internal.ProxyChannelState.ApiVersions;
import io.kroxylicious.proxy.internal.ProxyChannelState.SelectingServer;
import io.kroxylicious.proxy.internal.codec.FrameOversizedException;
import io.kroxylicious.proxy.model.VirtualCluster;
import io.kroxylicious.proxy.service.HostPort;

import edu.umd.cs.findbugs.annotations.NonNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ProxyChannelStateMachineTest {

    public static final HostPort BROKER_ADDRESS = new HostPort("localhost", 9092);
    public static final HAProxyMessage HA_PROXY_MESSAGE = new HAProxyMessage(HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, HAProxyProxiedProtocol.TCP4,
            "1.1.1.1", "2.2.2.2", 46421, 9092);
    private ProxyChannelStateMachine proxyChannelStateMachine;
    private KafkaProxyBackendHandler backendHandler;
    private KafkaProxyFrontendHandler frontendHandler;

    @BeforeEach
    void setUp() {
        proxyChannelStateMachine = new ProxyChannelStateMachine();
        backendHandler = mock(KafkaProxyBackendHandler.class);
        frontendHandler = mock(KafkaProxyFrontendHandler.class);
    }

    private void stateHolderInClientActive() {
        proxyChannelStateMachine.state = new ProxyChannelState.ClientActive();
        proxyChannelStateMachine.backendHandler = null;
        proxyChannelStateMachine.frontendHandler = frontendHandler;
    }

    private void stateHolderInHaProxy() {
        proxyChannelStateMachine.state = new ProxyChannelState.HaProxy(HA_PROXY_MESSAGE);
        proxyChannelStateMachine.backendHandler = null;
        proxyChannelStateMachine.frontendHandler = frontendHandler;
    }

    private void stateHolderInApiVersionsState() {
        proxyChannelStateMachine.state = new ProxyChannelState.ApiVersions(null, null, null);
        proxyChannelStateMachine.frontendHandler = frontendHandler;
    }

    private void stateHolderInSelectingServer() {
        proxyChannelStateMachine.state = new ProxyChannelState.SelectingServer(null, null, null);
        proxyChannelStateMachine.backendHandler = null;
        proxyChannelStateMachine.frontendHandler = frontendHandler;
    }

    private ProxyChannelState.Connecting stateHolderInConnecting() {
        ProxyChannelState.Connecting state = new ProxyChannelState.Connecting(null, null, null);
        proxyChannelStateMachine.state = state;
        proxyChannelStateMachine.backendHandler = backendHandler;
        proxyChannelStateMachine.frontendHandler = frontendHandler;
        return state;
    }

    private ProxyChannelState.Forwarding stateHolderInForwarding() {
        var forwarding = new ProxyChannelState.Forwarding(null, null, null);
        proxyChannelStateMachine.state = forwarding;
        proxyChannelStateMachine.backendHandler = backendHandler;
        proxyChannelStateMachine.frontendHandler = frontendHandler;
        return forwarding;
    }

    private void stateHolderInClosing(Throwable cause) {
        proxyChannelStateMachine.state = new ProxyChannelState.Closing(cause, false, false);
        proxyChannelStateMachine.backendHandler = backendHandler;
        proxyChannelStateMachine.frontendHandler = frontendHandler;
    }

    private void stateHolderInClosed() {
        proxyChannelStateMachine.state = new ProxyChannelState.Closed();
        proxyChannelStateMachine.backendHandler = backendHandler;
        proxyChannelStateMachine.frontendHandler = frontendHandler;
    }

    @NonNull
    private static DecodedRequestFrame<ApiVersionsRequestData> apiVersionsRequest() {
        return new DecodedRequestFrame<>(
                ApiVersionsResponseData.ApiVersion.HIGHEST_SUPPORTED_VERSION,
                1,
                false,
                new RequestHeaderData(),
                new ApiVersionsRequestData()
                        .setClientSoftwareName("mykafkalib")
                        .setClientSoftwareVersion("1.0.0"));
    }

    @NonNull
    private static DecodedRequestFrame<MetadataRequestData> metadataRequest() {
        return new DecodedRequestFrame<>(
                MetadataRequestData.HIGHEST_SUPPORTED_VERSION,
                0,
                false,
                new RequestHeaderData(),
                new MetadataRequestData());
    }

    @NonNull
    private static DecodedResponseFrame<MetadataResponseData> metadataResponse() {
        return new DecodedResponseFrame<>(
                MetadataRequestData.HIGHEST_SUPPORTED_VERSION,
                0,
                new ResponseHeaderData(),
                new MetadataResponseData());
    }

    @Test
    void shouldBlockClientReads() {
        // Given
        proxyChannelStateMachine.frontendHandler = frontendHandler;

        // When
        proxyChannelStateMachine.onServerUnwritable();
        proxyChannelStateMachine.onServerUnwritable();

        // Then
        verify(frontendHandler, times(1)).blockClientReads();
    }

    @Test
    void shouldUnblockClientReads() {
        // Given
        proxyChannelStateMachine.frontendHandler = frontendHandler;
        proxyChannelStateMachine.clientReadsBlocked = true;

        // When
        proxyChannelStateMachine.onServerWritable();
        proxyChannelStateMachine.onServerWritable();

        // Then
        verify(frontendHandler, times(1)).unblockClientReads();
    }

    @Test
    void shouldBlockServerReads() {
        // Given
        proxyChannelStateMachine.backendHandler = backendHandler;

        // When
        proxyChannelStateMachine.onClientUnwritable();
        proxyChannelStateMachine.onClientUnwritable();

        // Then
        verify(backendHandler, times(1)).blockServerReads();
    }

    @Test
    void shouldCloseOnClientRuntimeException() {
        // Given
        proxyChannelStateMachine.frontendHandler = frontendHandler;
        proxyChannelStateMachine.backendHandler = backendHandler;
        RuntimeException cause = new RuntimeException("Oops!");

        // When
        proxyChannelStateMachine.onClientException(cause, true);

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class);
        verify(backendHandler).inClosing();
        verify(frontendHandler).inClosing(ArgumentMatchers.notNull(UnknownServerException.class));
    }

    @Test
    void shouldCloseOnClientFrameOversizedException() {
        // Given
        proxyChannelStateMachine.frontendHandler = frontendHandler;
        proxyChannelStateMachine.backendHandler = backendHandler;
        RuntimeException cause = new DecoderException(new FrameOversizedException(2, 1));

        // When
        proxyChannelStateMachine.onClientException(cause, true);

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class);
        verify(backendHandler).inClosing();
        verify(frontendHandler).inClosing(ArgumentMatchers.notNull(InvalidRequestException.class));
    }

    @Test
    void shouldCloseOnServerRuntimeException() {
        // Given
        proxyChannelStateMachine.frontendHandler = frontendHandler;
        proxyChannelStateMachine.backendHandler = backendHandler;
        RuntimeException cause = new RuntimeException("Oops!");

        // When
        proxyChannelStateMachine.onServerException(cause);

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class);
        verify(backendHandler).inClosing();
        verify(frontendHandler).inClosing(cause);
    }

    @Test
    void shouldUnblockServerReads() {
        // Given
        proxyChannelStateMachine.backendHandler = backendHandler;
        proxyChannelStateMachine.serverReadsBlocked = true;

        // When
        proxyChannelStateMachine.onClientWritable();
        proxyChannelStateMachine.onClientWritable();

        // Then
        verify(backendHandler, times(1)).unblockServerReads();
    }

    @Test
    void inStartStateShouldCallInClientActive() {
        // Given

        // When
        proxyChannelStateMachine.onClientActive(frontendHandler);

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.ClientActive.class);
        verify(frontendHandler, times(1)).inClientActive();
    }

    @Test
    void inClientActiveShouldCaptureHaProxyState() {
        // Given
        stateHolderInClientActive();
        var dp = mock(SaslDecodePredicate.class);

        // When
        proxyChannelStateMachine.onClientRequest(dp, HA_PROXY_MESSAGE);

        // Then
        assertThat(proxyChannelStateMachine.state)
                .asInstanceOf(InstanceOfAssertFactories.type(ProxyChannelState.HaProxy.class))
                .extracting(ProxyChannelState.HaProxy::haProxyMessage)
                .isSameAs(HA_PROXY_MESSAGE);
        verifyNoInteractions(dp);
    }

    @Test
    void inClientActiveShouldBufferWhenOnClientMetadataRequest() {
        // Given
        stateHolderInClientActive();
        var msg = metadataRequest();
        var dp = mock(SaslDecodePredicate.class);

        // When
        proxyChannelStateMachine.onClientRequest(dp, msg);

        // Then
        assertThat(proxyChannelStateMachine.state)
                .isInstanceOf(ProxyChannelState.SelectingServer.class);
        verifyNoInteractions(dp);
        verify(frontendHandler).inSelectingServer();
        verify(frontendHandler).bufferMsg(msg);
        verifyNoMoreInteractions(frontendHandler);
    }

    @Test
    void inHaProxyShouldBufferWhenOnClientApiVersionsRequest() {
        // Given
        stateHolderInHaProxy();
        var msg = apiVersionsRequest();
        var dp = new SaslDecodePredicate(false);

        // When
        proxyChannelStateMachine.onClientRequest(dp, msg);

        // Then
        assertThat(proxyChannelStateMachine.state)
                .isInstanceOf(ProxyChannelState.SelectingServer.class);
        verify(frontendHandler).inSelectingServer();
        verify(frontendHandler).bufferMsg(msg);
        verifyNoMoreInteractions(frontendHandler);
    }

    @Test
    void inHaProxyShouldCloseOnHaProxyMsg() {
        // Given
        stateHolderInHaProxy();
        var dp = new SaslDecodePredicate(false);

        // When
        proxyChannelStateMachine.onClientRequest(dp, HA_PROXY_MESSAGE);

        // Then
        assertThat(proxyChannelStateMachine.state)
                .isInstanceOf(ProxyChannelState.Closing.class);
        verify(frontendHandler).inClosing(null);
    }

    @Test
    void inHaProxyShouldBufferWhenOnClientMetadataRequest() {
        // Given
        stateHolderInHaProxy();
        var msg = metadataRequest();
        var dp = mock(SaslDecodePredicate.class);

        // When
        proxyChannelStateMachine.onClientRequest(dp, msg);

        // Then
        assertThat(proxyChannelStateMachine.state)
                .isInstanceOf(ProxyChannelState.SelectingServer.class);
        verifyNoInteractions(dp);
        verify(frontendHandler).inSelectingServer();
        verify(frontendHandler).bufferMsg(msg);
        verifyNoMoreInteractions(frontendHandler);
    }

    @Test
    void inApiVersionsShouldCloseOnClientActive() {
        // Given
        stateHolderInApiVersionsState();

        // When
        proxyChannelStateMachine.onClientActive(frontendHandler);

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class);
        verify(frontendHandler).inClosing(null);
    }

    @Test
    void inApiVersionsShouldBuffer() {
        // Given
        stateHolderInApiVersionsState();
        var msg = metadataRequest();
        SaslDecodePredicate dp = mock(SaslDecodePredicate.class);

        // When
        proxyChannelStateMachine.onClientRequest(dp, msg);

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.SelectingServer.class);
        verify(frontendHandler).bufferMsg(msg);
    }

    @Test
    void inApiVersionsShouldCloseOnHaProxyMessage() {
        // Given
        stateHolderInApiVersionsState();
        var dp = mock(SaslDecodePredicate.class);

        // When
        proxyChannelStateMachine.onClientRequest(dp, HA_PROXY_MESSAGE);

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class);
        verify(frontendHandler).inClosing(null);
        verifyNoInteractions(backendHandler);
        verifyNoInteractions(dp);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void inClientActiveShouldTransitionToApiVersionsOrSelectingServer(boolean handlingSasl) {
        // Given
        stateHolderInClientActive();
        var msg = apiVersionsRequest();

        // When
        proxyChannelStateMachine.onClientRequest(
                new SaslDecodePredicate(handlingSasl),
                msg);

        // Then
        if (handlingSasl) {
            var stateAssert = assertThat(proxyChannelStateMachine.state)
                    .asInstanceOf(InstanceOfAssertFactories.type(ApiVersions.class));
            stateAssert
                    .extracting(ApiVersions::clientSoftwareName).isEqualTo("mykafkalib");
            stateAssert
                    .extracting(ApiVersions::clientSoftwareVersion).isEqualTo("1.0.0");
        }
        else {
            var stateAssert = assertThat(proxyChannelStateMachine.state)
                    .asInstanceOf(InstanceOfAssertFactories.type(SelectingServer.class));
            stateAssert
                    .extracting(SelectingServer::clientSoftwareName).isEqualTo("mykafkalib");
            stateAssert
                    .extracting(SelectingServer::clientSoftwareVersion).isEqualTo("1.0.0");
        }
        verify(frontendHandler).bufferMsg(msg);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void inSelectingServerShouldTransitionToConnectingWhenOnNetFilterInitiateConnectCalled(boolean configureSsl) throws SSLException {
        // Given
        HostPort brokerAddress = new HostPort("localhost", 9092);
        stateHolderInSelectingServer();
        var filters = List.<FilterAndInvoker> of();
        var vc = mock(VirtualCluster.class);
        doReturn(configureSsl ? Optional.of(SslContextBuilder.forClient().build()) : Optional.empty()).when(vc).getUpstreamSslContext();
        var nf = mock(NetFilter.class);

        // When
        proxyChannelStateMachine.onNetFilterInitiateConnect(brokerAddress, filters, vc, nf);

        // Then
        assertThat(proxyChannelStateMachine.state)
                .isInstanceOf(ProxyChannelState.Connecting.class);
        verify(frontendHandler).inConnecting(eq(brokerAddress), eq(filters), notNull(KafkaProxyBackendHandler.class));
        assertThat(proxyChannelStateMachine.backendHandler).isNotNull();
    }

    @Test
    void inClientActiveShouldCloseWhenOnNetFilterInitiateConnectCalled() {
        // Given
        HostPort brokerAddress = new HostPort("localhost", 9092);
        stateHolderInClientActive();
        var filters = List.<FilterAndInvoker> of();
        var vc = mock(VirtualCluster.class);
        var nf = mock(NetFilter.class);

        // When
        proxyChannelStateMachine.onNetFilterInitiateConnect(brokerAddress, filters, vc, nf);

        // Then
        assertThat(proxyChannelStateMachine.state)
                .isInstanceOf(ProxyChannelState.Closing.class);
        verify(frontendHandler).inClosing(null);
        assertThat(proxyChannelStateMachine.backendHandler).isNull();
    }

    @Test
    void inConnectingShouldCloseWhenOnNetFilterInitiateConnect() {
        // Given
        stateHolderInConnecting();

        var filters = List.<FilterAndInvoker> of();
        var vc = mock(VirtualCluster.class);
        var nf = mock(NetFilter.class);

        // When
        proxyChannelStateMachine.onNetFilterInitiateConnect(BROKER_ADDRESS, filters, vc, nf);

        // Then
        assertThat(proxyChannelStateMachine.state)
                .isInstanceOf(ProxyChannelState.Closing.class);
        verify(frontendHandler).inClosing(null);
        verify(backendHandler).inClosing();
    }

    @Test
    void inConnectingShouldTransitionWhenOnServerActiveCalled() {
        // Given
        stateHolderInConnecting();

        // When
        proxyChannelStateMachine.onServerActive();

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Forwarding.class);

        verify(frontendHandler).inForwarding();
        verifyNoInteractions(backendHandler);
    }

    @Test
    void inConnectingShouldBufferRequests() {
        // Given
        var state = stateHolderInConnecting();

        // When
        DecodedRequestFrame<MetadataRequestData> msg = metadataRequest();
        proxyChannelStateMachine.onClientRequest(new SaslDecodePredicate(false), msg);

        // Then
        verify(frontendHandler).bufferMsg(msg);
        assertThat(proxyChannelStateMachine.state).isEqualTo(state);
    }

    @Test
    void inClientActiveShouldCloseWhenOnServerActiveCalled() {
        // Given
        stateHolderInClientActive();

        // When
        proxyChannelStateMachine.onServerActive();

        // Then
        assertThat(proxyChannelStateMachine.state)
                .isInstanceOf(ProxyChannelState.Closing.class);
        verify(frontendHandler).inClosing(null);
    }

    @Test
    void inForwardingShouldForwardClientRequests() {
        // Given
        var serverCtx = mock(ChannelHandlerContext.class);
        SaslDecodePredicate dp = mock(SaslDecodePredicate.class);
        var forwarding = stateHolderInForwarding();
        var msg = metadataRequest();

        // When
        proxyChannelStateMachine.onClientRequest(dp, msg);

        // Then
        assertThat(proxyChannelStateMachine.state).isSameAs(forwarding);
        verifyNoInteractions(frontendHandler);
        verifyNoInteractions(dp);
        verifyNoInteractions(serverCtx);
        verify(backendHandler).forwardToServer(msg);
    }

    @Test
    void inForwardingShouldForwardServerResponses() {
        // Given
        var serverCtx = mock(ChannelHandlerContext.class);
        SaslDecodePredicate dp = mock(SaslDecodePredicate.class);
        var forwarding = stateHolderInForwarding();
        var msg = metadataResponse();

        // When
        proxyChannelStateMachine.forwardToClient(msg);

        // Then
        assertThat(proxyChannelStateMachine.state).isSameAs(forwarding);
        verify(frontendHandler).forwardToClient(msg);
        verifyNoInteractions(dp);
        verifyNoInteractions(serverCtx);
        verifyNoInteractions(backendHandler);
    }

    @Test
    void inForwardingShouldTransitionToClosingOnServerInactive() {
        // Given
        stateHolderInForwarding();
        doAnswer(invocation -> assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class)).when(frontendHandler).inClosing(null);
        doNothing().when(backendHandler).inClosing();

        // When
        proxyChannelStateMachine.onServerInactive();

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class);
        verify(frontendHandler).inClosing(null);
        verify(backendHandler).inClosing();
    }

    @Test
    void inForwardingShouldTransitionToClosingOnClientInactive() {
        // Given
        stateHolderInForwarding();
        doAnswer(invocation -> assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class)).when(frontendHandler).inClosing(null);

        // When
        proxyChannelStateMachine.onClientInactive();

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class);
        verify(frontendHandler).inClosing(null);
        verify(backendHandler).inClosing();
    }

    @Test
    void shouldNotTransitionToClosingMultipleTimes() {
        // Given
        stateHolderInClosing(null);

        // When
        proxyChannelStateMachine.onServerInactive();

        // Then
        verifyNoInteractions(frontendHandler, backendHandler);
    }

    @Test
    void shouldNotTransitionFromClosedToClosing() {
        // Given
        stateHolderInClosed();

        // When
        proxyChannelStateMachine.onServerInactive();

        // Then
        verifyNoInteractions(frontendHandler, backendHandler);
    }

    @Test
    void inForwardingShouldTransitionToClosingOnServerException() {
        // Given
        stateHolderInForwarding();
        final IllegalStateException illegalStateException = new IllegalStateException("She canny take it any more, captain");
        doNothing().when(backendHandler).inClosing();

        // When
        proxyChannelStateMachine.onServerException(illegalStateException);

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class);
        verify(frontendHandler).inClosing(illegalStateException);
        verify(backendHandler).inClosing();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void inForwardingShouldTransitionToClosingOnClientException(boolean tlsEnabled) {
        // Given
        stateHolderInForwarding();
        final ApiException expectedException = Errors.UNKNOWN_SERVER_ERROR.exception();
        final IllegalStateException illegalStateException = new IllegalStateException("She canny take it any more, captain");
        doAnswer(invocation -> assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class)).when(frontendHandler)
                .inClosing(expectedException);
        doNothing().when(backendHandler).inClosing();

        // When
        proxyChannelStateMachine.onClientException(illegalStateException, tlsEnabled);

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class);
        verify(frontendHandler).inClosing(expectedException);
        verify(backendHandler).inClosing();
    }

    @Test
    void shouldNotTransitionFromClosingToClosedOnServerClosed() {
        // Given
        stateHolderInClosing(null);

        // When
        proxyChannelStateMachine.onServerClosed();

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class);
    }

    @Test
    void shouldNotTransitionFromClosingToClosedOnClientClosed() {
        // Given
        stateHolderInClosing(null);

        // When
        proxyChannelStateMachine.onClientClosed();

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closing.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void shouldOnlyTransitionToClosedOnceBothClientAndServerAreClosed(boolean serverFirst) {
        // Given
        stateHolderInClosing(null);
        onClose(serverFirst);

        // When
        onClose(!serverFirst);

        // Then
        assertThat(proxyChannelStateMachine.state).isInstanceOf(ProxyChannelState.Closed.class);
    }

    private void onClose(boolean server) {
        if (server) {
            proxyChannelStateMachine.onServerClosed();
        }
        else {
            proxyChannelStateMachine.onClientClosed();
        }
    }
}
