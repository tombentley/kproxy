/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.aws.kms;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import io.kroxylicious.kms.service.Kms;
import io.kroxylicious.kms.service.KmsService;

public class AwsKmsService implements KmsService<AwsOptions, AwsKeyRef, AwsEdek> {

    @Override
    public Kms<AwsKeyRef, AwsEdek> buildKms(AwsOptions options) {
        return null;
    }

    @Override
    public Serializer<AwsKeyRef> keyRefSerializer() {
        return null;
    }

    @Override
    public Deserializer<AwsEdek> edekDeserializer() {
        return null;
    }
}
