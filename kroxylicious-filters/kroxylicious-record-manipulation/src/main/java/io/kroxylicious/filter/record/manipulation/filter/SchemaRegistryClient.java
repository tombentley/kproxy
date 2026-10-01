/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.util.concurrent.CompletionStage;

public interface SchemaRegistryClient {

    // ContentResult carries raw schema bytes, so an array component is intentional here.
    @SuppressWarnings("ArrayRecordComponent")
    record ContentResult(ContentId contentId, byte[] contentBytes) {}

    CompletionStage<ContentResult> getContentById(ContentId contentId);

    record VersionsResult(
                          GlobalId globalId,
                          String groupId,
                          String artifactId,
                          String version,
                          ContentId contentId) {

    }

    CompletionStage<VersionsResult> artifactIdForGlobalId(GlobalId globalId);
}
