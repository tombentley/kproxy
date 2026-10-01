/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record VersionSearchResults(List<SearchedVersion> versions) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchedVersion(String groupId, String artifactId, String version, long globalId, long contentId) {}
}
