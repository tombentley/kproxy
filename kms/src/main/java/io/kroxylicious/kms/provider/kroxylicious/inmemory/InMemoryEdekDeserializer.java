/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

class InMemoryEdekDeserializer implements Deserializer<InMemoryEdek> {

    @Override
    public InMemoryEdek deserialize(String topic, byte[] data) {
        var x = new ByteArrayInputStream(data);
        var y = new DataInputStream(x);
        try {
            var bits = y.readInt();
            var ivLength = y.readInt();
            var iv = new byte[ivLength];
            y.read(iv);
            var edekLength = y.readInt();
            var edek = new byte[edekLength];
            y.read(edek);
            return new InMemoryEdek(bits, iv, edek);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
