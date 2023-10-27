/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.azure.keyvault;

public class KeyVaultKeyRef {
    private final String id;

    public KeyVaultKeyRef(String id) {
        this.id = id;
    }

}
