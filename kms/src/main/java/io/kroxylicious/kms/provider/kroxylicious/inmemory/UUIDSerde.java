/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import java.nio.ByteBuffer;
import java.util.UUID;

import io.kroxylicious.kms.service.Serde;

import edu.umd.cs.findbugs.annotations.NonNull;

class UUIDSerde implements Serde<UUID> {

    @Override
    public UUID deserialize(@NonNull ByteBuffer buffer) {
        var msb = buffer.getLong();
        var lsb = buffer.getLong();
        return new UUID(msb, lsb);
    }

    @Override
    public int sizeOf(UUID uuid) {
        return 16;
    }

    @Override
    public void serialize(UUID uuid, @NonNull ByteBuffer buffer) {
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
    }
}
