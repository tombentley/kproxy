/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.schema.identification;

import java.io.IOException;
import java.util.List;

import org.apache.kafka.common.header.Header;

import io.kroxylicious.filter.transformation.RecordDataLocation;
import io.kroxylicious.filter.transformation.TransformationInputStream;

public interface InputSchemaIdentification {
    WireSchemaId schemaIdFromData(List<Header> headers, RecordDataLocation site, TransformationInputStream data) throws IOException;
}
