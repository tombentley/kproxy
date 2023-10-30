/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import java.nio.ByteBuffer;
import java.util.UUID;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.Ser;

class UUIDSerde implements Ser<UUID>, De<UUID> {

    @Override
    public UUID deserialize(ByteBuffer buffer) {
        var msb = buffer.getLong();
        var lsb = buffer.getLong();
        return new UUID(msb, lsb);
    }

    @Override
    public int sizeOf(UUID uuid) {
        return 16;
    }

    @Override
    public void serialize(UUID uuid, ByteBuffer buffer) {
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
    }
}
