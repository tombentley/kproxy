/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

public class RecordTransformationException extends RuntimeException {

    public RecordTransformationException(String message) {
        super(message);
    }

    public RecordTransformationException(Throwable cause) {
        super(cause);
    }

    public RecordTransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
