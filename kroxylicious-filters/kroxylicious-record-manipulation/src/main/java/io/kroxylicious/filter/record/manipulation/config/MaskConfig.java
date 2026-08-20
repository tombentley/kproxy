/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A JSON-Schema-like description of how a JSON value should be masked or generated,
 * recursively describing nested {@code properties} and array {@code items}.
 * @param type the JSON type this mask applies to, e.g. {@code object}, {@code array}, {@code string}, {@code integer}
 * @param properties for {@code type: object}, the mask for each named property
 * @param items for {@code type: array}, the mask applied to each element
 * @param value a fixed replacement/generated value
 * @param choose a set of values to choose a replacement/generated value from
 * @param random configuration for generating a random replacement/generated value
 * @param hmac configuration for replacing the value with its HMAC
 * @param encrypt configuration for replacing the value with its ciphertext
 * @param decrypt configuration for replacing the value with its plaintext
 */
public record MaskConfig(String type,
                         Map<String, MaskConfig> properties,
                         MaskConfig items,
                         JsonNode value,
                         List<Object> choose,
                         RandomMaskConfig random,
                         HmacMaskConfig hmac,
                         EncryptMaskConfig encrypt,
                         DecryptMaskConfig decrypt) {
    // TODO some way to represent the identity function
    // TODO some way to fail/throw
}
