/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Sequentially applies a number of other transformations.
 */
public class SequentialTransformation implements BufferTransformation {

    private final List<BufferTransformation> transformations;

    private SequentialTransformation(List<BufferTransformation> transformations) {
        this.transformations = Objects.requireNonNull(transformations);
    }

    static BufferTransformation of(List<BufferTransformation> transformations) {
        int numTransformation = transformations.size();
        if (numTransformation == 0) {
            return BufferTransformation.identity();
        }
        else if (numTransformation == 1) {
            return transformations.get(0);
        }
        return new SequentialTransformation(transformations);
    }

    @Override
    public void transform(TransformationInputStream in, TransformationOutputStream out) throws IOException {
        for (int i = 0; i < transformations.size(); i++) {
            var transformation = transformations.get(i);
            transformation.transform(in, out);
            if (i < transformations.size() - 1) {
                in = out.flip();
            }
        }
    }
}
