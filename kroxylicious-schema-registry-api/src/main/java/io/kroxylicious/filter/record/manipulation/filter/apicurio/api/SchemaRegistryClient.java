/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio.api;

import java.util.concurrent.CompletionStage;

public interface SchemaRegistryClient {

    CompletionStage<ContentResult> contentById(ContentId contentId);

    CompletionStage<ContentResult> contentByGav(GavCoordinates gavCoordinates);

    CompletionStage<VersionsResult> versionsForGlobalId(GlobalId globalId);

}
