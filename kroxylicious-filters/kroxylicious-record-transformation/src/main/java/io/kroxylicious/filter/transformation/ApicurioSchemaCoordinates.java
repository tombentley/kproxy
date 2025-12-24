/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

public record ApicurioSchemaCoordinates(String groupId, String artifactId, String version) implements WireSchemaId {}
