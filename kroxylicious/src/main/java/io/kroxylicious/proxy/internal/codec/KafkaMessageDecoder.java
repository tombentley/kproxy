/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.internal.codec;

import java.util.List;

import org.slf4j.Logger;

import io.kroxylicious.proxy.frame.Frame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * Abstraction for request and response decoders.
 */
public abstract class KafkaMessageDecoder extends ByteToMessageDecoder {

    protected abstract Logger log();

    public KafkaMessageDecoder() {
    }

    @Override
    public synchronized void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (in.readableBytes() > 4) {
            try {
                int sof = in.readerIndex();
                int frameSize = in.readInt();
                int readable = in.readableBytes();
                if (log().isTraceEnabled()) { // avoid boxing
                    log().trace("{}: Frame of {} bytes ({} readable)", ctx, frameSize, readable);
                }
                // TODO handle too-large frames
                if (readable >= frameSize) { // We can read the whole frame
                    var idx = in.readerIndex();
                    out.add(decodeHeaderAndBody(ctx,
                            in.readSlice(frameSize), // Prevent decodeHeaderAndBody() from reading beyond the frame
                            frameSize));
                    log().trace("{}: readable: {}, having read {}", ctx, in.readableBytes(), in.readerIndex() - idx);
                    if (in.readerIndex() - idx != frameSize) {
                        throw new RuntimeException("decodeHeaderAndBody did not read all of the buffer " + in);
                    }
                }
                else {
                    in.readerIndex(sof);
                    break;
                }
            }
            catch (Exception e) {
                log().error("{}: Error in decoder", ctx, e);
                throw e;
            }
        }
    }

    protected abstract Frame decodeHeaderAndBody(ChannelHandlerContext ctx, ByteBuf in, int length);

}
