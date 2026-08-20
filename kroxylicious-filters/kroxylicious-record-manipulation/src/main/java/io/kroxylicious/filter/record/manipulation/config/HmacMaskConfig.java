/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

/**
 * Configuration for computing an HMAC of a value with the key identified by {@code keyId}.
 * @param keyId the identifier of the HMAC key
 */
public record HmacMaskConfig(String keyId) {}
