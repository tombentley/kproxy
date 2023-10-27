/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kms.provider.kroxylicious.inmemory;

import java.util.Arrays;
import java.util.Objects;

public record InMemoryEdek(
                           int numAuthBits,
                           byte[] iv,
                           byte[] edek) {
    @SuppressWarnings("checkstyle:NeedBraces")
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

    @Override
    public int hashCode() {
        int result = Objects.hash(numAuthBits);
        result = 31 * result + Arrays.hashCode(iv);
        result = 31 * result + Arrays.hashCode(edek);
        return result;
    }

    @Override
    public String toString() {
        return "InMemoryEdek{" +
                "numAuthBits=" + numAuthBits +
                ", iv=" + Arrays.toString(iv) +
                ", edek=" + Arrays.toString(edek) +
                '}';
    }
}
