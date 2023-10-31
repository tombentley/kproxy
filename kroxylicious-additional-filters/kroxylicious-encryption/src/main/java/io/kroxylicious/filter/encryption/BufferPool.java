/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReferenceArray;

interface BufferPool {

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

    static BufferPool directAllocating() {
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
        AtomicReferenceArray<ArrayBlockingQueue<ByteBuffer>> freeLists = new AtomicReferenceArray<>(32);

        return new BufferPool() {
            @Override
            public ByteBuffer acquire(int size) {
                int index = ceil(size);
                ArrayBlockingQueue<ByteBuffer> freeList = freeList(index);
                return freeList.poll();
            }

            private ArrayBlockingQueue<ByteBuffer> freeList(int index) {
                var freeList = freeLists.updateAndGet(index, x -> {
                    if (x == null) {
                        x = new ArrayBlockingQueue<>(10);
                    }
                    return x;
                });
                return freeList;
            }

            private int ceil(int size) {
                return size & (size - 1);
            }

            @Override
            public void release(ByteBuffer buffer) {
                buffer.position(0);
                int index = ceil(buffer.capacity());
                freeList(index).offer(buffer);
            }
        };
    }
}
