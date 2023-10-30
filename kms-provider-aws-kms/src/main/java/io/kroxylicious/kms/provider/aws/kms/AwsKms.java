/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.aws.kms;

import java.util.concurrent.CompletionStage;

import javax.crypto.SecretKey;

import edu.umd.cs.findbugs.annotations.NonNull;

import io.kroxylicious.kms.service.De;
import io.kroxylicious.kms.service.DekPair;
import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.Ser;

import software.amazon.awssdk.services.kms.KmsClient;

public class AwsKms implements Kms<AwsKeyRef, AwsEdek> {

    private final KmsClient client;

    AwsKms(KmsClient client) {
        this.client = client;
    }

    @NonNull
    @Override
    public CompletionStage<AwsEdek> generateDek(@NonNull AwsKeyRef kekRef) {
        return null;
    }

    @NonNull
    @Override
    public CompletionStage<DekPair<AwsEdek>> generateDekPair(@NonNull AwsKeyRef kekRef) {
        return null;
    }

    @NonNull
    @Override
    public CompletionStage<SecretKey> decryptEdek(@NonNull AwsKeyRef kek, @NonNull AwsEdek edek) {
        return null;
    }

    @NonNull
    @Override
    public De<AwsKeyRef> keyRefDeserializer() {
        return null;
    }

    @NonNull
    @Override
    public Ser<AwsEdek> edekSerializer() {
        return null;
    }

    @NonNull
    @Override
    public Ser<AwsKeyRef> keyRefSerializer() {
        return null;
    }

    @NonNull
    @Override
    public De<AwsEdek> edekDeserializer() {
        return null;
    }
}
