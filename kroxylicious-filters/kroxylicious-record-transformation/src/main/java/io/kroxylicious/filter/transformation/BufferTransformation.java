/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;

/**
 * A transformation on a buffer
 */
public interface BufferTransformation {

    BufferTransformation IDENTITY = TransformationInputStream::transferTo;

    BufferTransformation ZERO = (in, out) -> {};

    /**
     * Returns the identity transformation.
     * The output of the identity transformation is the input.
     * @return The identity transformation
     */
    static BufferTransformation identity() {
        return IDENTITY;
    }

    /**
     * Returns the zero transformation.
     * The output of the zero transformation is always empty.
     * @return The zero transformation
     */
    static BufferTransformation zero() {
        return ZERO;
    }

    void transform(TransformationInputStream in, TransformationOutputStream out) throws IOException;
}
