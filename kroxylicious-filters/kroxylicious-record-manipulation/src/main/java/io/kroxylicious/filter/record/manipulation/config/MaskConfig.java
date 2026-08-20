/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

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
