/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.inband;

import org.junit.jupiter.api.Test;

import io.kroxylicious.kms.provider.kroxylicious.inmemory.InMemoryKms;
import io.kroxylicious.kms.provider.kroxylicious.inmemory.InMemoryKmsService;

class InBandDekCacheTest {

    private final InMemoryKmsService service = new InMemoryKmsService();
    private final InMemoryKms inMemoryKms = service.buildKms(new InMemoryKmsService.Config(12, 128));

    @Test
    void roundTrip() {
        var key = inMemoryKms.generateKey();

        var cache = new InBandDekCache<>(inMemoryKms, BufferPool.allocating());

        // var stage = (CompletableFuture<? extends Object>) null; // cache.forKekId(key);
        // assertThat(stage).isCompleted();
        //
        // var context = stage.toCompletableFuture().join();
        //
        // var in = ByteBuffer.wrap("input".getBytes(StandardCharsets.UTF_8));
        // var out = ByteBuffer.allocate(1024);
        // context.encode(in, out);
        // out.flip();
        //
        // var resolvedContextStage = cache.resolve(out);
        // assertThat(resolvedContextStage).isCompleted();
        // var encryptor = resolvedContextStage.toCompletableFuture().join();
        // var roundTripped = ByteBuffer.allocate(5);
        // encryptor.decrypt(out, roundTripped);
        // assertThat(new String(roundTripped.array(), StandardCharsets.UTF_8)).isEqualTo("input");

    }
}
