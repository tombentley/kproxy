/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Random;
import java.util.function.Supplier;

/**
 * A provider of strings of a random length composed of
 * codepoints taken at random from a given alphabet.
 */
public class RandomStringSupplier implements Supplier<String> {

    private final Random prng;
    private final String alphabet;
    private final int minLengthInclusive;
    private final int maxLengthExclusive;

    /**
     * A provider of strings of a random length
     * between {@code minLengthInclusive} and {@code maxLengthExclusive} composed of
     * codepoints taken at random from the given {@code alphabet}
     * @param prng The source of randomness
     * @param alphabet The codepoints to pick from
     * @param minLengthInclusive The minimum length of the string (inclusive)
     * @param maxLengthExclusive The maximum length of the string (exclusive)
     */
    public RandomStringSupplier(Random prng, String alphabet, int minLengthInclusive, int maxLengthExclusive) {
        if (minLengthInclusive < 0) {
            throw new IllegalArgumentException("minLengthInclusive (" + minLengthInclusive + ") must be >= 0");
        }
        if (minLengthInclusive >= maxLengthExclusive) {
            throw new IllegalArgumentException("minLengthInclusive (" + minLengthInclusive + ") must be < maxLengthExclusive (" + maxLengthExclusive + ")");
        }
        this.prng = prng;
        this.alphabet = alphabet;
        this.minLengthInclusive = minLengthInclusive;
        this.maxLengthExclusive = maxLengthExclusive;
    }

    @Override
    public String get() {
        var codePoints = alphabet.codePoints().toArray();
        // TODO this should really be using codepoints, not chars
        var length = prng.nextInt(minLengthInclusive, maxLengthExclusive);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.appendCodePoint(codePoints[prng.nextInt(alphabet.length())]);
        }
        return sb.toString();
    }
}
