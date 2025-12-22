/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;

public interface DatumSerializer<T> {

    Class<T> acceptedType();

    // TODO this doesn't really model the variability of header schema ids
    // We actually need two types
    // one for prefixes (which uses the simpler type RecordStream)
    // and one for headers (which needs to use
    void serialize(Datum<T> datum, TransformationOutputStream out) throws IOException;
}
