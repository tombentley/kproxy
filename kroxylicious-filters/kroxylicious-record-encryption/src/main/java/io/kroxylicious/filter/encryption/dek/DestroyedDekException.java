/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.dek;

/**
 * Indicates that a {@link Dek} couldn't be used because it was destroyed.
 */
public class DestroyedDekException extends DekException {

    public DestroyedDekException() {
        super();
    }
}
