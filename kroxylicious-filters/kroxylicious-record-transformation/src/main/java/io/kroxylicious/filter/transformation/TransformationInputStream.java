/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class TransformationInputStream extends InputStream {
    protected final ByteBuffer byteBuffer;
    private int mark;
    private int readlimit;

    TransformationInputStream(ByteBuffer buf) {
        byteBuffer = buf;
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

    public byte readByte() throws IOException {
        var b = read();
        if (b == -1) {
            throw new EOFException();
        }
        else {
            return (byte) b;
        }
    }

    public int readInt() throws IOException {
        if (this.byteBuffer.remaining() < 4) {
            throw new EOFException();
        }
        return this.byteBuffer.getInt();
    }

    public long readLong() throws IOException {
        if (this.byteBuffer.remaining() < 8) {
            throw new EOFException();
        }
        return this.byteBuffer.getLong();
    }

    /**
     * Transfer the remaining bytes from this stream to the given stream.
     * @param outputStream The stream to tranfers this stream's bytes to.
     */
    public void transferTo(TransformationOutputStream outputStream) throws IOException {
        // TODO note that this does not override `public long transferTo(OutputStream out) throws IOException` but it should do
        // TODO this is a copy. It would be nice if we could make make it take ownership
        outputStream.write(this.byteBuffer);
    }
}
