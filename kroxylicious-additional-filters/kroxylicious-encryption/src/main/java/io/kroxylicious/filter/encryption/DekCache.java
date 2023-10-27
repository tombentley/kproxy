/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface DekCache<K, I> {

    Ser<I> serializer();



    /**
     * Gets the current encryptors for the given KEKs
     * @param keks A mapping from topic name to KEK ids
     * @return The encryptors for each topic
     */
    CompletableFuture<Map<String, Map.Entry<I, AesGcmEncryptor>>> encryptors(Map<String, K> keks);
}
