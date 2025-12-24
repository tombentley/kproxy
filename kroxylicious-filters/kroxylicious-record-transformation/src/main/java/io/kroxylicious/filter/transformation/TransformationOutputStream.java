/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * A {@link ByteBuffer}-backed OutputStream
 * with some optimizations for record transformation.
 * The internal buffer will be reallocated and copied if the initial
 * capacity proves undersized.
 *
 * <h3>Design note</h3>
 * There is a mismatch between the Record API that Kafka offers us, which uses ByteBuffer,
 * and the APIs of common codecs like Avro and Protobuf, which don't provide support for
 * ByteBuffer. Our Serializer and Deserializer APIs are written in terms of OutputStream,
 * simplifying their implementation.
 */
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

    void write(ByteBuffer byteBuffer) throws IOException {
        checkWritable();
        ensureCapacity(byteBuffer.remaining());
        this.byteBuffer.put(byteBuffer);
    }

    /**
     * Try to accept ownership of the given buffer, replacing this stream's existing buffer
     * if no data has been written to it yet.
     *
     * If data has already been written to this stream then this call is equivalent to
     * calling {@link #write(ByteBuffer)}.
     *
     * The caller of this method should "own" the given buffer, and be able to ensure the bytes
     * it refers to won't be subsequently modified.
     * This is provided to support a zero copy optimization when bytes would
     * otherwise be copied from a {@link TransformationInputStream} to a {@code TransformationOutputStream}.
     *
     * @param byteBuffer The buffer to accept
     * @return true if the buffer was accepted.
     * @throws IOException
     */
    boolean accept(ByteBuffer byteBuffer) throws IOException {
        checkWritable();
        if (this.byteBuffer.position() == 0) {
            this.byteBuffer = byteBuffer;
            this.writable = false;
            return true;
        }
        else {
            write(byteBuffer);
            return false;
        }
    }

    /**
     * Close this output stream returning an input stream for the contents that was written.
     * @return An input stream.
     * @throws IOException
     */
    public TransformationInputStream flip() throws IOException {
        return new TransformationInputStream(toByteBuffer());
    }

    /**
     * Close this output stream returning a byte buffer for the data that was written.
     * @return A buffer of the data that was written.
     * @throws IOException
     */
    public ByteBuffer toByteBuffer() throws IOException {
        close();
        return byteBuffer.duplicate();
    }

    /**
     * Unclose this stream, rewinding this streams internal buffer.
     * This is an optimization to avoid needing buffer allocations
     * for each record to be transformed.
     */
    void reset() {
        writable = true;
        byteBuffer.rewind();
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

