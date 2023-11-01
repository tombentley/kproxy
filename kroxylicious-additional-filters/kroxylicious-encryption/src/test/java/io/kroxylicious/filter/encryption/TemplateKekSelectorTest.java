/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.kroxylicious.kms.provider.kroxylicious.inmemory.InMemoryKms;
import io.kroxylicious.kms.provider.kroxylicious.inmemory.InMemoryKmsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateKekSelectorTest {

    @Test
    void shouldRejectUnknownPlaceholders() {
        var e = assertThrows(IllegalArgumentException.class, () -> new TemplateKekSelector<>(null, "foo-${topicId}-bar"));
        assertEquals("Unknown template parameter: topicId", e.getMessage());
    }


    @Test
    void shouldResolveWhenAliasExists() throws ExecutionException, InterruptedException {
        InMemoryKms kms = InMemoryKmsService.newInstance().buildKms(new InMemoryKmsService.Config(12, 128));
        var selector = new TemplateKekSelector<>(kms, "topic-${topicName}");

        var kek = kms.generateKey();
        kms.createAlias(kek, "topic-my-topic");
        var map = selector.selectKek(Set.of("my-topic")).toCompletableFuture().get();
        assertEquals(kek, map.get("my-topic"));
    }

    @Test
    void shouldThrowWhenAliasDoesNotExist() throws ExecutionException, InterruptedException {
        InMemoryKms kms = InMemoryKmsService.newInstance().buildKms(new InMemoryKmsService.Config(12, 128));
        var selector = new TemplateKekSelector<>(kms, "topic-${topicName}");

        var map = selector.selectKek(Set.of("my-topic")).toCompletableFuture().get();
        assertNull(map.get("my-topic"));
    }

}
