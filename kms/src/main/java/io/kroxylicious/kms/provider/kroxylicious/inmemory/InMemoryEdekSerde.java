/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import java.nio.ByteBuffer;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.Ser;

class InMemoryEdekSerde implements Ser<InMemoryEdek>, De<InMemoryEdek> {

    @Override
    public InMemoryEdek deserialize(ByteBuffer buffer) {
        short numAuthBits = (short) (buffer.get() & 0xFF);
        var ivLength = (short) (buffer.get() & 0xFF);
        var iv = new byte[ivLength];
        buffer.get(iv);
        int edekLength = buffer.limit() - buffer.position();
        var edek = new byte[edekLength];
        buffer.get(edek);
        return new InMemoryEdek(numAuthBits, iv, edek);
    }

    @Override
    public int sizeOf(InMemoryEdek inMemoryEdek) {
        return Byte.BYTES // Auth tag: NIST.SP.800-38D §5.2.1.2 suggests max tag length is 128
                + Byte.BYTES // IV length: NIST.SP.800-38D §8.2 certainly doesn't _limit_ IV to 96 bits
                + inMemoryEdek.iv().length
                + inMemoryEdek.edek().length;
    }

    @Override
    public void serialize(InMemoryEdek inMemoryEdek, ByteBuffer buffer) {
        buffer.put((byte) (inMemoryEdek.numAuthBits() & 0xFF));
        buffer.put((byte) (inMemoryEdek.iv().length & 0xFF));
        buffer.put(inMemoryEdek.iv());
        buffer.put(inMemoryEdek.edek());
    }
}
