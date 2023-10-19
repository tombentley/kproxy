/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.aws.kms;

import java.util.Objects;

public class AwsKeyRef {

    private final String keyId;

    AwsKeyRef(String keyId) {
        this.keyId = Objects.requireNonNull(keyId);
    }
}
