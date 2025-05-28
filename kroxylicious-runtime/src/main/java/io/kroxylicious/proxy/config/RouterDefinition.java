/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.kroxylicious.proxy.plugin.PluginImplConfig;
import io.kroxylicious.proxy.plugin.PluginImplName;
import io.kroxylicious.proxy.router.RouterFactory;

import edu.umd.cs.findbugs.annotations.Nullable;

public record RouterDefinition(@JsonProperty(value = "name", required = true) String routerName,
                               @PluginImplName(RouterFactory.class) @JsonProperty(required = true) String type,
                               @PluginImplConfig(implNameProperty = "type") Object config,
                               @Nullable List<RouteDefinition> routes) {

    public RouterDefinition {
        if (routes != null) {
            Configuration.checkUniqueness("routes", routes, "name", RouteDefinition::routeName);
        }
    }
}
