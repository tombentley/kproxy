/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.ibm.keyprotect;

import io.kroxylicious.kms.service.KmsService;

import edu.umd.cs.findbugs.annotations.NonNull;

public class IbmKeyProtectService implements KmsService<IbmOptions, IbmKeyRef, IbmEdek> {
    @NonNull
    public IbmKeyProtect buildKms(IbmOptions options) {
        return new IbmKeyProtect(options);
    }

}
