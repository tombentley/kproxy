/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.internal;

import java.net.InetAddress;

/**
 * Sent as a Netty "User Event" by the {@link PROXYNetHandler}
 * to handlers interested in how the user has connected to any proxy
 * intermediating the connection between client and this proxy.
 */
public class PROXYDecodeEvent {
    private final InetAddress srcAddr;
    private final int srcPort;
    private final InetAddress destAddr;
    private final int destPort;

    public PROXYDecodeEvent(InetAddress srcAddr, int srcPort,
                            InetAddress destAddr, int destPort) {
        this.srcAddr = srcAddr;
        this.srcPort = srcPort;
        this.destAddr = destAddr;
        this.destPort = destPort;
    }

    public InetAddress srcAddr() {
        return srcAddr;
    }

    public int srcPort() {
        return srcPort;
    }

    public InetAddress destAddr() {
        return destAddr;
    }

    public int destPort() {
        return destPort;
    }
}
