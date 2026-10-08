/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.kroxylicious.filter.record.manipulation.filter.service.ServiceFactory;
import io.kroxylicious.proxy.plugin.PluginImplConfig;
import io.kroxylicious.proxy.plugin.PluginImplName;

public record NamedService(@JsonProperty(required = true) String name,
                           @JsonProperty(required = true) @PluginImplName(ServiceFactory.class) String type,
                           @PluginImplConfig(implNameProperty = "type") Object config) {}
