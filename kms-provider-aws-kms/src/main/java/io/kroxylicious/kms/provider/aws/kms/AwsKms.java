/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.aws.kms;

import java.util.concurrent.CompletionStage;
import javax.crypto.SecretKey;

import software.amazon.awssdk.services.kms.KmsClient;

import io.kroxylicious.kms.service.DekGenerator;
import io.kroxylicious.kms.service.Kms;

public class AwsKms implements Kms<AwsKeyRef, AwsEdek>,
        DekGenerator<AwsKeyRef, AwsEdek> {

    private final KmsClient client;

    AwsKms(KmsClient client) {
        this.client = client;
    }

    @Override
    public CompletionStage<AwsEdek> generateDek(AwsKeyRef kekRef) {
        return null;
    }

    @Override
    public CompletionStage<SecretKey> decryptEdek(AwsKeyRef kek, AwsEdek edek) {
        return null;
    }
}
