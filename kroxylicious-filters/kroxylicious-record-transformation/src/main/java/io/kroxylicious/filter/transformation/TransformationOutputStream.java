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

    TransformationOutputStream(int initialCapacity) {
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

    private void checkWritable() throws IOException {
        if (!writable) {
            throw new IOException("stream is no longer writable");
        }
    }

    @Override
    public void write(int b) throws IOException {
        checkWritable();
        if (!byteBuffer.hasRemaining()) {
            realloc((int) (GROWTH_FACTOR * (byteBuffer.capacity() + 1)));
        }
        byteBuffer.put((byte) b);
    }
    
    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        checkWritable();
        if (byteBuffer.remaining() < len) {
            realloc((int) (GROWTH_FACTOR * (byteBuffer.capacity() + len)));
        }
        byteBuffer.put(bytes, off, len);
    }

    @Override
    public void close() throws IOException {
        writable = false;
        byteBuffer = byteBuffer.asReadOnlyBuffer();
    }

    public void write(ByteBuffer byteBuffer) throws IOException {
        checkWritable();
        this.byteBuffer.put(byteBuffer);
    }

    public TransformationInputStream flip() throws IOException {
        return new TransformationInputStream(toByteBuffer());
    }

    public ByteBuffer toByteBuffer() throws IOException {
        close();
        return byteBuffer.flip();
    }

    public void writeInt(int schemaId) throws IOException {
        checkWritable();
        byteBuffer.putInt(schemaId);
    }

    public void writeLong(long schemaId) throws IOException {
        checkWritable();
        byteBuffer.putLong(schemaId);
    }
}

