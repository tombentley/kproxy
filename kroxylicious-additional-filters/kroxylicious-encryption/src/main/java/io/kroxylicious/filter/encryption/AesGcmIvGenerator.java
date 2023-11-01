/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import java.security.SecureRandom;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * RBG-based construction of the Initialization Vector for AES-GCM.
 * The random field is 96 bits and the free field is empty.
 *
 * @see "§8.2.2 of NIST SP.800-38D: Recommendation for Block
 * Cipher Modes of Operation:
 * Galois/Counter Mode (GCM)
 * and GMAC"
 */
class AesGcmIvGenerator {

    private int low;
    private int mid;
    private int hi;

    AesGcmIvGenerator(@NonNull SecureRandom rng) {
        low = rng.nextInt();
        mid = rng.nextInt();
        hi = rng.nextInt();
    }

    int sizeBytes() {
        return 12;
    }

    void generateIv(byte[] iv) {
        iv[0] = (byte) (hi >> 24);
        iv[1] = (byte) (hi >> 16);
        iv[2] = (byte) (hi >> 8);
        iv[3] = (byte) (hi);
        iv[4] = (byte) (mid >> 24);
        iv[5] = (byte) (mid >> 16);
        iv[6] = (byte) (mid >> 8);
        iv[7] = (byte) (mid);
        iv[8] = (byte) (low >> 24);
        iv[9] = (byte) (low >> 16);
        iv[10] = (byte) (low >> 8);
        iv[11] = (byte) (low);
        try {
            low = Math.addExact(low, 1);
        }
        catch (ArithmeticException e) {
            low = Integer.MIN_VALUE;
            try {
                mid = Math.addExact(mid, 1);
            }
            catch (ArithmeticException e2) {
                mid = Integer.MIN_VALUE;
                try {
                    hi = Math.addExact(hi, 1);
                }
                catch (ArithmeticException e3) {
                    hi = Integer.MIN_VALUE;
                }
            }
        }
    }
}
