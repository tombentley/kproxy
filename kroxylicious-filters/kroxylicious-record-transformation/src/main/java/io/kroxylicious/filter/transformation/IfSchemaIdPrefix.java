/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.util.Objects;

/**
 * Apply another {@link BufferTransformation} if the input buffer is prefixed
 * with a given schema identifier (a.k.a. "Confluent Schema Registry"-style).
 * The given schema identifier is not passed on to the given other transformation.
 * If the input buffer doesn't have the given schema id
 * (including if it lacks any schema id) then TODO
 */
public class IfSchemaIdPrefix implements BufferTransformation {

    public static final int MAGIC_BYTE = 0;
    private final boolean fourByte;
    private final long expectedSchemaId;
    private final BufferTransformation transformation;

    public IfSchemaIdPrefix(int numBytes,
                            long expectedSchemaId,
                            BufferTransformation transformation) {
        if (numBytes == 4) {
            this.fourByte = true;
            this.expectedSchemaId = Math.toIntExact(expectedSchemaId);
        }
        else if (numBytes == 8) {
            this.fourByte = false;
            this.expectedSchemaId = expectedSchemaId;
        }
        else {
            throw new IllegalArgumentException("numBytes must be either 4 or 8");
        }

        this.transformation = Objects.requireNonNull(transformation);
    }

    @Override
    public void transform(TransformationInputStream in,
                          TransformationOutputStream out) throws IOException {
        var maybeMagic = in.read();
        if (maybeMagic == MAGIC_BYTE) {
            if (isSchemaMatch(in)) {
                transformation.transform(in, out);
            }
            else {
                BufferTransformation.identity().transform(in, out);
            }
        }
        else {
            BufferTransformation.identity().transform(in, out);
        }
    }

    private boolean isSchemaMatch(TransformationInputStream in) throws IOException {
        boolean schemaMatch;
        if (fourByte) {
            var actual = in.readInt();
            schemaMatch = actual == (int) expectedSchemaId;
        }
        else {
            var actual = in.readLong();
            schemaMatch = actual == expectedSchemaId;
        }
        return schemaMatch;
    }
}
