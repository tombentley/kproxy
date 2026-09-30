/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.ops.jsonschema.networknt.jackson3;

public class DataNotSchemaValidException extends RuntimeException {
    public DataNotSchemaValidException(String string) {
        super(string);
    }
}
