/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.authentication;

/**
 * Enumerates the disposition of a consumer of authenticated principals.
 */
public enum PrincipalDisposition {
    /** A principal for the peer is requested, but not required. */
    REQUESTED,
    /** A principal for the peer is required, but mutual authentication is not required. */
    REQUIRED,
    /**
     * Mutual authentication is required:
     * Principal for both the proxy and the peer are required.
     */
    MUTUAL_REQUIRED
}
