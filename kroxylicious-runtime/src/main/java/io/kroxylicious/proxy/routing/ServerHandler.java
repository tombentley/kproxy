/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.routing;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    boolean pendingServerFlushes;
    ChannelHandlerContext serverCtx;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        this.serverCtx = ctx;
        super.channelRegistered(ctx);
    }
}
