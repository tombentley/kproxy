/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.service;

public class UnknownAliasException extends KmsException {

    public UnknownAliasException(String alias) {
        super(alias);
    }
}
