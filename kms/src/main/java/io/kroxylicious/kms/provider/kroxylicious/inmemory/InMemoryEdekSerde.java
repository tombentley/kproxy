/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.Ser;

import java.nio.ByteBuffer;

class InMemoryEdekSerde implements Ser<InMemoryEdek>, De<InMemoryEdek> {

    @Override
    public InMemoryEdek deserialize(ByteBuffer buffer) {
        var numAuthBits = buffer.getShort();
        var ivLength = buffer.getShort();
        var iv = new byte[ivLength];
        buffer.get(iv);
        int edekLength = buffer.limit() - buffer.position();
        var edek = new byte[edekLength];
        buffer.get(edek);
        return new InMemoryEdek(numAuthBits, iv, edek);
    }

    @Override
    public int sizeOf(InMemoryEdek inMemoryEdek) {
        return Short.BYTES + Short.BYTES + inMemoryEdek.iv().length + inMemoryEdek.edek().length;
    }

    @Override
    public void serialize(InMemoryEdek inMemoryEdek, ByteBuffer buffer) {
        buffer.putShort((short) inMemoryEdek.numAuthBits());
        buffer.putShort((short) inMemoryEdek.iv().length);
        buffer.put(inMemoryEdek.iv());
        buffer.put(inMemoryEdek.edek());
    }
}
