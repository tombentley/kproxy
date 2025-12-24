/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A {@link ByteBuffer}-backed InputStream
 * with some optimizations for record transformation.
 * The internal buffer will be reallocated and copied if the initial
 * capacity proves undersized.
 */
public class TransformationInputStream extends InputStream {
    private ByteBuffer byteBuffer;
    private int mark = -1;
    private int readlimit = -1;

    public TransformationInputStream(ByteBuffer buf) {
        byteBuffer = buf.duplicate();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    @Override
    public int available() {
        return byteBuffer.remaining();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public synchronized void mark(int readlimit) {
        this.mark = byteBuffer.position();
        this.readlimit = mark + readlimit;
    }

    @Override
    public synchronized void reset() throws IOException {
        if (mark == -1) {
            throw new IOException("mark not set");
        }
        else if (readlimit < byteBuffer.position()) {
            throw new IOException("readlimit exceeded");
        }
        byteBuffer.position(this.mark);
    }

    @Override
    public int read() throws IOException {
        if (this.byteBuffer.hasRemaining()) {
            return this.byteBuffer.get() & 0xFF;
        }
        return -1;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (!this.byteBuffer.hasRemaining()) {
            return -1;
        }
        len = Math.min(len, this.byteBuffer.remaining());
        this.byteBuffer.get(bytes, off, len);
        return len;
    }

    /**
     * Reads the next byte of data from the input stream.
     * If no byte is available because the end of the stream has been reached,
     * EOFException is thrown.
     * @return the next byte of data
     * @throws IOException
     */
    public byte readByte() throws IOException {
        var b = read();
        if (b == -1) {
            throw new EOFException();
        }
        else {
            return (byte) b;
        }
    }

    /**
     * Reads the next 32-bit integer of data from the input stream assuming big endian byte order.
     * If no int is available because the end of the stream has been, or would be, reached
     * EOFException is thrown.
     * @return the next int of data
     * @throws IOException
     */
    public int readInt() throws IOException {
        if (this.byteBuffer.remaining() < 4) {
            throw new EOFException();
        }
        return this.byteBuffer.getInt();
    }

    /**
     * Reads the next 64-bit long of data from the input stream assuming big endian byte order.
     * If no long is available because the end of the stream has been, or would be, reached
     * EOFException is thrown.
     * @return the next long of data
     * @throws IOException
     */
    public long readLong() throws IOException {
        if (this.byteBuffer.remaining() < 8) {
            throw new EOFException();
        }
        return this.byteBuffer.getLong();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        if (out instanceof TransformationOutputStream tout) {
            // We can avoid some intermediate buffers used by super's impl
            int remaining = byteBuffer.remaining();
            tout.write(this.byteBuffer);
            return remaining;
        }
        else {
            return super.transferTo(out);
        }
    }

    /**
     * Transfer the remaining bytes from this stream to the given stream.
     * @param outputStream The stream to tranfers this stream's bytes to.
     */
    public void offer(TransformationOutputStream outputStream) throws IOException {
        // TODO this should not be public, but it needs to be accessible to ByteSerializer
        if (outputStream.accept(this.byteBuffer)) {
            this.byteBuffer = this.byteBuffer.asReadOnlyBuffer();
        }
    }
}
