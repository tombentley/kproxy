/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.router.topic;

import java.util.HashMap;
import java.util.Map;

public class GroupRoutingTableImpl implements GroupRoutingTable {

    private Map<String, String> routingTable = new HashMap<>();
    String defaultRoute;

    GroupRoutingTableImpl(Map<String, String> groupToRoute, String defaultRoute) {
        // Implementation here
        this.routingTable = groupToRoute;
        this.defaultRoute = defaultRoute;
    }

    @Override
    public String getRouteFor(String group) {
        return routingTable.getOrDefault(group, defaultRoute);
    }
}
