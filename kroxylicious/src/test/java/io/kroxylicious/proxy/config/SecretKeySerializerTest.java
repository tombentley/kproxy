/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config;

import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import static org.assertj.core.api.Assertions.assertThat;

class SecretKeySerializerDeserializerTest {

    private static final ObjectMapper OBJECT_MAPPER;
    private static final String TEST_KEY_ALGORITHM = "AES";
    private static final SecretKey TEST_KEY;
    private static final String TEST_KEY_BASE64;

    static {
        OBJECT_MAPPER = new ObjectMapper().registerModule(new SimpleModule().addSerializer(SecretKey.class, new SecretKeySerializer()))
                .registerModule(new SimpleModule().addDeserializer(SecretKey.class, new SecretKeyDeserializer()));
        try {
            TEST_KEY = javax.crypto.KeyGenerator.getInstance(TEST_KEY_ALGORITHM).generateKey();
            TEST_KEY_BASE64 = OBJECT_MAPPER.convertValue(TEST_KEY.getEncoded(), String.class);
        }
        catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    void roundTrip() {
        var tree = OBJECT_MAPPER.valueToTree(TEST_KEY);
        assertThat(tree.fieldNames()).toIterable().containsExactly("algo", "key");
        assertThat(tree.get("algo").asText()).isEqualTo(TEST_KEY_ALGORITHM);
        assertThat(tree.get("key").asText()).isEqualTo(TEST_KEY_BASE64);

        var key = OBJECT_MAPPER.convertValue(tree, SecretKey.class);
        assertThat(key).isEqualTo(TEST_KEY);
    }
}
