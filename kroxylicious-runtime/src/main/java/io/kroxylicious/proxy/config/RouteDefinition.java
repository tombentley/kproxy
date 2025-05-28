/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

record RouteDefinition(@JsonProperty(value = "name", required = true) String routeName,
                       List<String> filters,
                       String router,
                       String cluster) {
    RouteDefinition {
        if ((router == null) == (cluster == null)) {
            throw new IllegalConfigurationException("Exactly one of 'router' or 'cluster' must be specified");
        }
    }
}
