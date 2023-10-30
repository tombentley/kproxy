/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.service;

import java.util.concurrent.CompletionStage;

import javax.crypto.SecretKey;

/**
 * Abstracts the KMS operations needed for Envelope Encryption
 * @param <K> The type of Key Encryption Key id.
 * @param <E> The type of encrypted Data Encryption Key.
 */
public interface Kms<K, E> {

    /**
     * A Data Encryption Key as both plaintext and encrypted.
     * @param edek The encrypted DEK.
     * @param dek The plaintext DEK.
     * @param <E> The type of encrypted DEK.
     */
    record DekPair<E>(E edek, SecretKey dek) {}

    /**
     * Asynchronously generates a new Data Encryption Key, returning it encrypted with the Key Encryption Key given by {@code kekRef}.
     * The returned DEK can later be decrypted with {@link Kms#decryptEdek(Object, Object)}.
     * @param kekRef The key encryption key used to encrypt the generated data encryption key.
     * @return A completion stage for the wrapped data encryption key.
     * @throws UnknownKeyException If the kek was not known to this KMS.
     * @throws InvalidKeyUsageException If the given kek was not intended for key wrapping.
     * @throws KmsException For other exceptions.
     */
    CompletionStage<E> generateDek(K kekRef);

    /**
     * Asynchronously generates a Data Encryption Key (DEK) and returns it together with the same DEK wrapped by the Key Encryption Key (KEK) given
     * by the {@code kekRef},
     * The returned encrypted DEK can later be decrypted with {@link Kms#decryptEdek(Object, Object)}.
     * @param kekRef The key encryption key used to encrypt the generated data encryption key.
     * @return A completion stage for the wrapped data encryption key.
     * @throws UnknownKeyException If the kek was not known to this KMS.
     * @throws InvalidKeyUsageException If the given kek was not intended for key wrapping.
     * @throws KmsException For other exceptions.
     */
    CompletionStage<DekPair<E>> generateDekPair(K kekRef);

    /**
     * Asynchronously decrypts a data encryption key that was {@linkplain #generateDek(Object) previously encrypted}.
     * @param kek The key encryption key.
     * @param edek The encrypted data encryption key.
     * @return A completion stage for the data encryption key
     * @throws UnknownKeyException If the kek was not known to this KMS.
     * @throws InvalidKeyUsageException If the given kek was not intended for key wrapping.
     * @throws KmsException For other exceptions
     */
    CompletionStage<SecretKey> decryptEdek(K kek, E edek);

    De<K> keyRefDeserializer();

    Ser<E> edekSerializer();

    Ser<K> keyRefSerializer();

    De<E> edekDeserializer();
}
