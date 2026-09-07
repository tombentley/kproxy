/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.function.BiFunction;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * Computes the Base64-encoded HMAC-SHA256 of a string, using a raw key drawn from the invocation's
 * {@link OpContext}. A fresh {@link Mac} is created per invocation - the key isn't known until then, and a
 * shared, cached instance would not be safe to reuse across concurrent invocations regardless.
 */
public class HmacStringFunction implements BiFunction<String, OpContext, String> {

    /**
     * Creates an instance.
     */
    public HmacStringFunction() {
    }

    @Override
    public String apply(String value, OpContext opContext) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(opContext.key(), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
