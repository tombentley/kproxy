/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import java.net.URI;

import io.kroxylicious.proxy.config.tls.Tls;

public record SchemaRegistry(URI url, Tls tls) {}
