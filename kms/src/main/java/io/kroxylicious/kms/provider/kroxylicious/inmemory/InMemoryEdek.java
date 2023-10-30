/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import java.util.Arrays;
import java.util.Objects;

record InMemoryEdek(
                    int numAuthBits,
                    byte[] iv,
                    byte[] edek) {
    /**
     * Overridden to provide deep equality on the {@code byte[]}.
     * @param o   the reference object with which to compare.
     * @return true iff this object is equal to the given object.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InMemoryEdek that = (InMemoryEdek) o;
        return numAuthBits == that.numAuthBits && Arrays.equals(iv, that.iv) && Arrays.equals(edek, that.edek);
    }

    /**
     * Overridden to provide a deep hashcode on the {@code byte[]}.
     * @return the has code.
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(numAuthBits);
        result = 31 * result + Arrays.hashCode(iv);
        result = 31 * result + Arrays.hashCode(edek);
        return result;
    }

    /**
     * Overridden to provide a deep {@code toString()} on the {@code byte[]}.
     * @return The string
     */
    @Override
    public String toString() {
        return "InMemoryEdek{" +
                "numAuthBits=" + numAuthBits +
                ", iv=" + Arrays.toString(iv) +
                ", edek=" + Arrays.toString(edek) +
                '}';
    }
}
