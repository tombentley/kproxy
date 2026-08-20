/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

/**
 * Configuration for generating a random number in {@code [min, max)} or a random string
 * of length in {@code [minLength, maxLength)} drawn from {@code alphabet}.
 * @param min the minimum generated number (inclusive)
 * @param max the maximum generated number (exclusive)
 * @param minLength the minimum generated string length (inclusive)
 * @param maxLength the maximum generated string length (exclusive)
 * @param alphabet the codepoints a generated string is drawn from
 */
public record RandomMaskConfig(int min, int max, int minLength, int maxLength, String alphabet) {}
