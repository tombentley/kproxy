/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.aws.kms;

import java.util.concurrent.CompletionStage;

import javax.crypto.SecretKey;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.Ser;

import software.amazon.awssdk.services.kms.KmsClient;

public class AwsKms implements Kms<AwsKeyRef, AwsEdek> {

    private final KmsClient client;

    AwsKms(KmsClient client) {
        this.client = client;
    }

    @Override
    public CompletionStage<AwsEdek> generateDek(AwsKeyRef kekRef) {
        return null;
    }

    @Override
    public CompletionStage<DekPair<AwsEdek>> generateDekPair(AwsKeyRef kekRef) {
        return null;
    }

    @Override
    public CompletionStage<SecretKey> decryptEdek(AwsKeyRef kek, AwsEdek edek) {
        return null;
    }

    @Override
    public De<AwsKeyRef> keyRefDeserializer() {
        return null;
    }

    @Override
    public Ser<AwsEdek> edekSerializer() {
        return null;
    }

    @Override
    public Ser<AwsKeyRef> keyRefSerializer() {
        return null;
    }

    @Override
    public De<AwsEdek> edekDeserializer() {
        return null;
    }
}
