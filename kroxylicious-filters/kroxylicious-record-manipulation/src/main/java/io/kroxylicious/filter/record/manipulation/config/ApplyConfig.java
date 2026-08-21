/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A single operation appearing in a {@link SchemaConfig}'s {@code apply} list, describing how a value
 * should be masked or generated.
 * @param value a fixed replacement/generated value
 * @param choose a set of values to choose a replacement/generated value from
 * @param random configuration for generating a random replacement/generated value
 * @param hmac configuration for replacing the value with its HMAC
 * @param encrypt configuration for replacing the value with its ciphertext
 * @param decrypt configuration for replacing the value with its plaintext
 * @param delete when {@code true}, removes this property/element instead of replacing it. Type-agnostic,
 *               unlike every other operation here.
 */
public record ApplyConfig(JsonNode value,
                          List<Object> choose,
                          RandomMaskConfig random,
                          HmacMaskConfig hmac,
                          EncryptMaskConfig encrypt,
                          DecryptMaskConfig decrypt,
                          Boolean delete) {
    // TODO some way to represent the identity function
    // TODO some way to fail/throw
}
