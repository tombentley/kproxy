/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.kafka.common.serialization.Serializer;

class InMemoryEdekSerializer implements Serializer<InMemoryEdek> {

    @Override
    public byte[] serialize(String topic, InMemoryEdek data) {
        int size = Integer.BYTES // auth bits
                + Integer.BYTES // length of IV
                + data.iv().length
                + data.edek().length;

        var out = new ByteArrayOutputStream(size);
        var os = new DataOutputStream(out);
        try {
            os.writeInt(data.numAuthBits());
            os.writeInt(data.iv().length);
            os.write(data.iv());
            os.writeInt(data.edek().length);
            os.write(data.edek());
            os.flush();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

}
