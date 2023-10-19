/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.service;

import java.util.concurrent.CompletionStage;

public interface DekGenerator<K, E> {

    /**
     * Asynchronously generates a Data Encryption Key, encrypts it with the Key Encryption Key given by the {@code kekRef}, and returns it.
     * The returned DEK can be decrypted with {@link Kms#decryptEdek(Object, Object)}.
     * @param kekRef The key encryption key used to encrypt the generated data encryption key.
     * @return A completion stage for the wrapped data encryption key.
     * @throws UnknownKeyException If the kek was not known to this KMS.
     * @throws InvalidKeyUsageException If the given kek was not intended for key wrapping.
     * @throws KmsException For other exceptions.
     */
    CompletionStage<E> generateDek(K kekRef);
}
