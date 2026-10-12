/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio.api;

public record GavCoordinates(String groupId,
                             String artifactId,
                             String version) {

    public static GavCoordinates latest(String groupId, String artifactId) {
        return new GavCoordinates(groupId, artifactId, "latest");
    }
}
