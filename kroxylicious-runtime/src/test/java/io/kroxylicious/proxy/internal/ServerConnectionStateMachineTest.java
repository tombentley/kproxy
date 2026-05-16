/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.internal;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

import io.kroxylicious.proxy.internal.util.ActivationToken;
import io.kroxylicious.proxy.service.HostPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ServerConnectionStateMachine} request buffering.
 */
class ServerConnectionStateMachineTest {

    private static final HostPort REMOTE = new HostPort("broker", 9092);

    private ServerConnectionStateMachine createScsm(boolean upstreamTls) {
        var ccsm = mock(ClientConnectionStateMachine.class);
        when(ccsm.sessionId()).thenReturn("test-session");
        when(ccsm.clusterName()).thenReturn("test-cluster");
        return new ServerConnectionStateMachine(
                REMOTE,
                upstreamTls,
                ccsm,
                mock(Counter.class),
                mock(Counter.class),
                mock(Timer.class),
                mock(ActivationToken.class));
    }

    @Test
    void sendRequestWhileConnectingShouldBuffer() {
        var scsm = createScsm(false);
        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Connecting.class);

        Object msg = new Object();
        scsm.sendRequest(msg);

        assertThat(scsm.serverMessagesInFlightCount).isZero();
    }

    @Test
    void sendRequestWhileActiveShouldForwardImmediately() {
        var scsm = createScsm(false);
        var channel = new EmbeddedChannel(scsm.backendHandler());
        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Active.class);

        Object msg = "test-request";
        scsm.sendRequest(msg);

        assertThat(scsm.serverMessagesInFlightCount).isEqualTo(1);
        assertThat(channel.<Object> readOutbound()).isEqualTo(msg);
    }

    @Test
    void onServerActiveShouldFlushPendingRequests() {
        var scsm = createScsm(false);

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
        when(ccsm.clusterName()).thenReturn("test-cluster");
        var scsm = new ServerConnectionStateMachine(
                REMOTE, false, ccsm,
                mock(Counter.class), mock(Counter.class),
                mock(Timer.class), mock(ActivationToken.class));

        scsm.sendRequest("req-1");
        verifyNoInteractions(ccsm);

        // channelActive → onServerActive → flush pending → ccsm callback
        new EmbeddedChannel(scsm.backendHandler());

        // At the point ccsm.onServerConnectionActive() was called,
        // the pending requests had already been flushed
        assertThat(scsm.serverMessagesInFlightCount).isEqualTo(1);
        verify(ccsm).onServerConnectionActive(scsm);
    }

    @Test
    void closedShouldReleasePendingRequests() {
        var scsm = createScsm(false);

        ByteBuf buf = Unpooled.buffer(4).writeInt(42);
        assertThat(buf.refCnt()).isEqualTo(1);
        scsm.sendRequest(buf);

        scsm.close();

        assertThat(buf.refCnt()).isZero();
    }

    @Test
    void closedWithNoPendingRequestsShouldNotFail() {
        var scsm = createScsm(false);

        scsm.close();

        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Closed.class);
    }

    @Test
    void exceptionWhileConnectingShouldReleasePendingRequests() {
        var scsm = createScsm(false);

        ByteBuf buf = Unpooled.buffer(4).writeInt(99);
        scsm.sendRequest(buf);
        assertThat(buf.refCnt()).isEqualTo(1);

        scsm.onServerException(new RuntimeException("connection failed"));

        assertThat(buf.refCnt()).isZero();
        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Closed.class);
    }

    @Test
    void onServerActiveWithNoPendingRequestsShouldNotFail() {
        var scsm = createScsm(false);
        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Connecting.class);

        // channelActive triggers onServerActive — no pending requests to flush
        new EmbeddedChannel(scsm.backendHandler());

        assertThat(scsm.state()).isInstanceOf(ServerConnectionState.Active.class);
        assertThat(scsm.serverMessagesInFlightCount).isZero();
    }

    @Test
    void pendingRequestsPreserveOrder() {
        var scsm = createScsm(false);

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
}
