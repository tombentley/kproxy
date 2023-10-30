/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public interface DekCache<K, E> {

    /**
     * Asynchronously gets the current DEK context for the Key Encryption Key with the given {@code kekId}.
     * @param kekId The KEK ids
     * @return The DEK context for this key
     */
    CompletionStage<DekContext<K>> forKekId(K kekId);

    /**
     * Asynchronously resolves the DEK context from (a prefix of) the given {@code buffer}.
     * Following a successful call the given {@code buffer}'s position should at the first byte following the DEK
     * @param buffer The buffer.
     * @return The DEK context for the given buffer.
     */
    CompletionStage<DekContext<K>> resolve(ByteBuffer buffer);
}
