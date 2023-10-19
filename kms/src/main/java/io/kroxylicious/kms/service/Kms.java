/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.service;

import java.util.concurrent.CompletionStage;
import javax.crypto.SecretKey;

public interface Kms<K, E> {

    /**
     * Asynchronously decrypts a data encryption key that was {@linkplain DekGenerator#generateDek(Object) previously encrypted}.
     * @param kek The key encryption key.
     * @param edek The encrypted data encryption key.
     * @return A completion stage for the data encryption key
     * @throws UnknownKeyException If the kek was not known to this KMS.
     * @throws InvalidKeyUsageException If the given kek was not intended for key wrapping.
     * @throws KmsException For other exceptions
     */
    CompletionStage<SecretKey> decryptEdek(K kek, E edek);


}
