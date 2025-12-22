/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;

/**
 * Adds a given schema id then applies another buffer transformation.
 */
public class PrefixWithSchemaId implements BufferTransformation {

    public static final int MAGIC_BYTE = 0x00;
    private final boolean fourByte;
    private final long schemaId;
    private final BufferTransformation transformation;

    public PrefixWithSchemaId(int numBytes, long schemaId, BufferTransformation transformation) {
        if (numBytes == 4) {
            this.fourByte = true;
        }
        else if (numBytes == 8) {
            this.fourByte = false;
        }
        else {
            throw new IllegalArgumentException("`numBytes` must be either 4 or 8");
        }
        this.schemaId = schemaId;
        if (transformation instanceof PrefixWithSchemaId) {
            throw new IllegalArgumentException("Adding multiple schema ids makes no sense");
        }
        this.transformation = transformation;
    }

    @Override
    public void transform(TransformationInputStream in, TransformationOutputStream out) throws IOException {
        out.write(MAGIC_BYTE);
        if (fourByte) {
            out.writeInt((int) schemaId);
        }
        else {
            out.writeLong(schemaId);
        }

        transformation.transform(in, out);

    }
}
