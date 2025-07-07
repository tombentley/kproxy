/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

/**
 * Represents a set of TLS credentials (certificate, private key and a list of intermediate certificates).
 * This interface is implemented by the runtime.
 */
public interface TlsCredentials {
    /* Intentionally empty: implemented and accessed only in the runtime */
}
