/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public interface BufferPool {

    ByteBuffer acquire(int size);

    void release(ByteBuffer buffer);

    static BufferPool allocating() {
        return new BufferPool() {
            @Override
            public ByteBuffer acquire(int size) {
                return ByteBuffer.allocate(size);
            }

            @Override
            public void release(ByteBuffer buffer) {

            }
        };
    }

    static BufferPool direct() {
        return new BufferPool() {
            @Override
            public ByteBuffer acquire(int size) {
                return ByteBuffer.allocateDirect(size);
            }

            @Override
            public void release(ByteBuffer buffer) {

            }
        };
    }

    static BufferPool pooled(int poolSize) {
        Queue<ByteBuffer> free = new ArrayBlockingQueue<>(100);

        return new BufferPool() {
            @Override
            public ByteBuffer acquire(int size) {
                int roundedSize = Math.max(size, size & (size - 1));
                return free.poll();
            }

            @Override
            public void release(ByteBuffer buffer) {
                buffer.position(0);
                free.offer(buffer);
            }
        };
    }
}
