/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class TransformationOutputStream extends OutputStream {

    private static final float GROWTH_FACTOR = 2f;

    private boolean writable = true;
    private ByteBuffer byteBuffer;

    public TransformationOutputStream(int initialCapacity) {
        this(ByteBuffer.allocate(initialCapacity));
    }

    private TransformationOutputStream(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    private void realloc(int capacity) {
        var newBuffer = ByteBuffer.allocate(capacity);
        newBuffer.put(byteBuffer.flip());
        byteBuffer = newBuffer;
    }

    private void ensureCapacity(int numBytes) {
        if (byteBuffer.remaining() < numBytes) {
            realloc((int) (GROWTH_FACTOR * (byteBuffer.capacity() + numBytes)));
        }
    }

    private void checkWritable() throws IOException {
        if (!writable) {
            throw new IOException("stream is no longer writable");
        }
    }

    @Override
    public void write(int b) throws IOException {
        checkWritable();
        ensureCapacity(1);
        byteBuffer.put((byte) b);
    }
    
    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        checkWritable();
        ensureCapacity(len);
        byteBuffer.put(bytes, off, len);
    }

    @Override
    public void close() throws IOException {
        if (writable) {
            byteBuffer = byteBuffer.asReadOnlyBuffer().flip();
        }
        writable = false;
    }

    public void write(ByteBuffer byteBuffer) throws IOException {
        checkWritable();
        ensureCapacity(byteBuffer.remaining());
        this.byteBuffer.put(byteBuffer);
    }

    public TransformationInputStream flip() throws IOException {
        return new TransformationInputStream(toByteBuffer());
    }

    public ByteBuffer toByteBuffer() throws IOException {
        close();
        return byteBuffer.duplicate();
    }

    public void writeInt(int value) throws IOException {
        checkWritable();
        ensureCapacity(4);
        byteBuffer.putInt(value);
    }

    public void writeLong(long value) throws IOException {
        checkWritable();
        ensureCapacity(8);
        byteBuffer.putLong(value);
    }
}

