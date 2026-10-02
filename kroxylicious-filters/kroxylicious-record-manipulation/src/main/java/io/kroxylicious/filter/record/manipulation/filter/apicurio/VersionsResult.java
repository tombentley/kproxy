/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio;

public record VersionsResult(
                             GlobalId globalId,
                             String groupId,
                             String artifactId,
                             String version,
                             ContentId contentId) {}
