/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.internal;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.embedded.EmbeddedChannel;

import io.kroxylicious.proxy.internal.tls.ServerTlsCredentialSupplierContextImpl;
import io.kroxylicious.proxy.internal.tls.TestCertificateUtil;
import io.kroxylicious.proxy.internal.tls.TlsCredentialsImpl;
import io.kroxylicious.proxy.internal.util.ActivationToken;
import io.kroxylicious.proxy.model.VirtualClusterModel;
import io.kroxylicious.proxy.service.HostPort;
import io.kroxylicious.proxy.tls.ServerTlsCredentialSupplier;
import io.kroxylicious.proxy.tls.TlsCredentials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ServerConnectionStateMachine}.
 */
class ServerConnectionStateMachineTest {

    private static final HostPort REMOTE = new HostPort("broker", 9092);
    private static final String CLUSTER_NAME = "test-cluster";

    private ServerConnectionStateMachine createScsm() {
        var ccsm = mock(ClientConnectionStateMachine.class);
        when(ccsm.sessionId()).thenReturn("test-session");
        when(ccsm.clusterName()).thenReturn(CLUSTER_NAME);
        var virtualCluster = mock(VirtualClusterModel.class);
        when(virtualCluster.getUpstreamSslContext()).thenReturn(Optional.empty());
        return new ServerConnectionStateMachine(
                REMOTE,
                ccsm,
                virtualCluster,
                CLUSTER_NAME,
                null,
                mock(Counter.class),
                mock(Timer.class),
                mock(ActivationToken.class));
    }

    @Test
    void sendRequestWhileConnectingShouldBuffer() {
        var scsm = createScsm();
        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Connecting.class);

        Object msg = new Object();
        scsm.sendRequest(msg);

        assertThat(scsm.serverMessagesInFlightCount).isZero();
    }

    @Test
    void sendRequestWhileActiveShouldForwardImmediately() {
        var scsm = createScsm();
        var channel = new EmbeddedChannel(scsm.backendHandler());
        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Active.class);

        Object msg = "test-request";
        scsm.sendRequest(msg);

        assertThat(scsm.serverMessagesInFlightCount).isEqualTo(1);
        assertThat(channel.<Object> readOutbound()).isEqualTo(msg);
    }

    @Test
    void onServerActiveShouldFlushPendingRequests() {
        var scsm = createScsm();

        scsm.sendRequest("req-1");
        scsm.sendRequest("req-2");
        assertThat(scsm.serverMessagesInFlightCount).isZero();

        // Registering the handler triggers channelActive → onServerActive → flush
        var channel = new EmbeddedChannel(scsm.backendHandler());
        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Active.class);
        assertThat(scsm.serverMessagesInFlightCount).isEqualTo(2);

        assertThat(channel.<Object> readOutbound()).isEqualTo("req-1");
        assertThat(channel.<Object> readOutbound()).isEqualTo("req-2");
        assertThat(channel.<Object> readOutbound()).isNull();
    }

    @Test
    void onServerActiveShouldFlushBeforePcsmCallback() {
        var ccsm = mock(ClientConnectionStateMachine.class);
        when(ccsm.sessionId()).thenReturn("test-session");
        when(ccsm.clusterName()).thenReturn(CLUSTER_NAME);
        var virtualCluster = mock(VirtualClusterModel.class);
        when(virtualCluster.getUpstreamSslContext()).thenReturn(Optional.empty());
        var scsm = new ServerConnectionStateMachine(
                REMOTE, ccsm, virtualCluster, CLUSTER_NAME, null,
                mock(Counter.class),
                mock(Timer.class), mock(ActivationToken.class));

        scsm.sendRequest("req-1");

        // channelActive → onServerActive → flush pending → ccsm callback
        new EmbeddedChannel(scsm.backendHandler());

        assertThat(scsm.serverMessagesInFlightCount).isEqualTo(1);
        verify(ccsm).onServerConnectionActive(scsm);
    }

    @Test
    void closedShouldReleasePendingRequests() {
        var scsm = createScsm();

        ByteBuf buf = Unpooled.buffer(4).writeInt(42);
        assertThat(buf.refCnt()).isEqualTo(1);
        scsm.sendRequest(buf);

        scsm.close();

        assertThat(buf.refCnt()).isZero();
    }

    @Test
    void closedWithNoPendingRequestsShouldNotFail() {
        var scsm = createScsm();

        scsm.close();

        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Closed.class);
    }

    @Test
    void exceptionWhileConnectingShouldReleasePendingRequests() {
        var scsm = createScsm();

        ByteBuf buf = Unpooled.buffer(4).writeInt(99);
        scsm.sendRequest(buf);
        assertThat(buf.refCnt()).isEqualTo(1);

        scsm.onServerException(new RuntimeException("connection failed"));

        assertThat(buf.refCnt()).isZero();
        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Closed.class);
    }

    @Test
    void onServerActiveWithNoPendingRequestsShouldNotFail() {
        var scsm = createScsm();
        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Connecting.class);

        new EmbeddedChannel(scsm.backendHandler());

        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Active.class);
        assertThat(scsm.serverMessagesInFlightCount).isZero();
    }

    @Test
    void pendingRequestsPreserveOrder() {
        var scsm = createScsm();

        for (int i = 0; i < 5; i++) {
            scsm.sendRequest("req-" + i);
        }

        var channel = new EmbeddedChannel(scsm.backendHandler());

        for (int i = 0; i < 5; i++) {
            assertThat(channel.<Object> readOutbound()).isEqualTo("req-" + i);
        }
        assertThat(channel.<Object> readOutbound()).isNull();
        assertThat(scsm.serverMessagesInFlightCount).isEqualTo(5);
    }

    // === TLS credential tests ===

    private ServerConnectionStateMachine createScsmWithMocks(ClientConnectionStateMachine ccsm,
                                                             VirtualClusterModel virtualCluster) {
        when(ccsm.sessionId()).thenReturn("test-session");
        when(ccsm.clusterName()).thenReturn(CLUSTER_NAME);
        when(virtualCluster.getUpstreamSslContext()).thenReturn(Optional.empty());
        return new ServerConnectionStateMachine(
                REMOTE,
                ccsm,
                virtualCluster,
                CLUSTER_NAME,
                null,
                mock(Counter.class),
                mock(Timer.class),
                mock(ActivationToken.class));
    }

    @Test
    void invokeTlsCredentialSupplierReportsSynchronousFailure() throws Exception {
        var ccsm = mock(ClientConnectionStateMachine.class);
        var virtualCluster = mock(VirtualClusterModel.class);
        RuntimeException failure = new RuntimeException("manager failed");
        when(virtualCluster.getTlsCredentialSupplierManager()).thenThrow(failure);
        var scsm = createScsmWithMocks(ccsm, virtualCluster);

        Channel channel = mock(Channel.class);
        ChannelPipeline pipeline = mock(ChannelPipeline.class);
        Method method = ServerConnectionStateMachine.class.getDeclaredMethod(
                "invokeTlsCredentialSupplier", HostPort.class, Channel.class, ChannelPipeline.class);
        method.setAccessible(true);

        method.invoke(scsm, REMOTE, channel, pipeline);

        verify(ccsm).onServerConnectionException(scsm, failure);
    }

    @Test
    void requestTlsCredentialsAppliesCredentialsOnEventLoop() {
        var ccsm = mock(ClientConnectionStateMachine.class);
        var virtualCluster = mock(VirtualClusterModel.class);
        var scsm = createScsmWithMocks(ccsm, virtualCluster);

        TlsCredentials badCreds = mock(TlsCredentials.class);
        ServerTlsCredentialSupplier supplier = context -> CompletableFuture.completedFuture(badCreds);
        var supplierContext = new ServerTlsCredentialSupplierContextImpl(null);
        Channel channel = mock(Channel.class);
        EventLoop eventLoop = mock(EventLoop.class);
        when(channel.eventLoop()).thenReturn(eventLoop);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(eventLoop).execute(any(Runnable.class));

        scsm.requestTlsCredentials(supplier, supplierContext, REMOTE, channel, mock(ChannelPipeline.class));

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ccsm).onServerConnectionException(any(ServerConnectionStateMachine.class), captor.capture());
        assertThat(captor.getValue())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected TlsCredentials implementation");
    }

    @Test
    void requestTlsCredentialsReportsSupplierFailureOnEventLoop() {
        var ccsm = mock(ClientConnectionStateMachine.class);
        var virtualCluster = mock(VirtualClusterModel.class);
        var scsm = createScsmWithMocks(ccsm, virtualCluster);

        RuntimeException failure = new RuntimeException("boom");
        ServerTlsCredentialSupplier supplier = context -> CompletableFuture.failedFuture(failure);
        var supplierContext = new ServerTlsCredentialSupplierContextImpl(null);
        Channel channel = mock(Channel.class);
        EventLoop eventLoop = mock(EventLoop.class);
        when(channel.eventLoop()).thenReturn(eventLoop);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(eventLoop).execute(any(Runnable.class));

        scsm.requestTlsCredentials(supplier, supplierContext, REMOTE, channel, mock(ChannelPipeline.class));

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ccsm).onServerConnectionException(any(ServerConnectionStateMachine.class), captor.capture());
        assertThat(captor.getValue())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to obtain TLS credentials")
                .hasCause(failure);
    }

    @Test
    void handleTlsCredentialSupplierResultReportsNullCredentials() {
        var ccsm = mock(ClientConnectionStateMachine.class);
        var virtualCluster = mock(VirtualClusterModel.class);
        var scsm = createScsmWithMocks(ccsm, virtualCluster);

        Channel channel = mock(Channel.class);
        ChannelPipeline pipeline = mock(ChannelPipeline.class);

        scsm.handleTlsCredentialSupplierResult(null, null, REMOTE, channel, pipeline);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ccsm).onServerConnectionException(any(ServerConnectionStateMachine.class), captor.capture());
        assertThat(captor.getValue())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TLS credential supplier returned null");
    }

    @Test
    void handleTlsCredentialSupplierResultAppliesCredentials() {
        var ccsm = mock(ClientConnectionStateMachine.class);
        var virtualCluster = mock(VirtualClusterModel.class);
        var scsm = createScsmWithMocks(ccsm, virtualCluster);

        TlsCredentials badCreds = mock(TlsCredentials.class);
        Channel channel = mock(Channel.class);
        ChannelPipeline pipeline = mock(ChannelPipeline.class);

        scsm.handleTlsCredentialSupplierResult(badCreds, null, REMOTE, channel, pipeline);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ccsm).onServerConnectionException(any(ServerConnectionStateMachine.class), captor.capture());
        assertThat(captor.getValue())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected TlsCredentials implementation");
    }

    @Test
    void applySslContextToChannelRejectsNonTlsCredentialsImpl() {
        var ccsm = mock(ClientConnectionStateMachine.class);
        var virtualCluster = mock(VirtualClusterModel.class);
        var scsm = createScsmWithMocks(ccsm, virtualCluster);

        TlsCredentials badCreds = mock(TlsCredentials.class);
        Channel channel = mock(Channel.class);
        ChannelPipeline pipeline = mock(ChannelPipeline.class);

        scsm.applySslContextToChannel(badCreds, REMOTE, channel, pipeline);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ccsm).onServerConnectionException(any(ServerConnectionStateMachine.class), captor.capture());
        assertThat(captor.getValue())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected TlsCredentials implementation");
    }

    @Test
    void applySslContextToChannelAddsSslHandlerWithValidCredentials() throws Exception {
        var ccsm = mock(ClientConnectionStateMachine.class);
        var virtualCluster = mock(VirtualClusterModel.class);
        var scsm = createScsmWithMocks(ccsm, virtualCluster);

        var keyAndCert = TestCertificateUtil.generateKeyStoreAndCert();
        var creds = new TlsCredentialsImpl(
                keyAndCert.privateKey(), new java.security.cert.X509Certificate[]{ keyAndCert.cert() });

        EmbeddedChannel channel = new EmbeddedChannel();

        when(virtualCluster.targetCluster()).thenReturn(mock(io.kroxylicious.proxy.config.TargetCluster.class));
        when(virtualCluster.targetCluster().tls()).thenReturn(Optional.empty());

        scsm.applySslContextToChannel(creds, REMOTE, channel, channel.pipeline());

        assertThat(channel.pipeline().get("ssl")).isNotNull();
        verify(ccsm, never()).onServerConnectionException(any(), any());

        channel.close();
    }
}
