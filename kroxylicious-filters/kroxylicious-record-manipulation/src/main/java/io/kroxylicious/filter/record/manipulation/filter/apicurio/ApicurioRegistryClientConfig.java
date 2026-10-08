/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio;

import java.net.URI;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.kroxylicious.proxy.config.tls.Tls;

import edu.umd.cs.findbugs.annotations.Nullable;

public record ApicurioRegistryClientConfig(
                                           @JsonProperty(required = true) URI registryUrl,
                                           Duration timeout,
                                           @Nullable Tls tls) {
    public static final int DEFAULT_TIMEOUT_MS = 20_000;

    public ApicurioRegistryClientConfig(
                                        @JsonProperty(required = true) URI registryUrl,
                                        @Nullable Duration timeout,
                                        @Nullable Tls tls) {
        this.registryUrl = registryUrl;
        if (timeout == null) {
            this.timeout = Duration.ofMillis(DEFAULT_TIMEOUT_MS);
        }
        else {
            this.timeout = timeout;
        }
        this.tls = tls;
    }
}
