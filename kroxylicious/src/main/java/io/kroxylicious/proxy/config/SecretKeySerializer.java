/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config;

import java.io.IOException;

import javax.crypto.SecretKey;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

class SecretKeySerializer extends StdSerializer<SecretKey> {
    protected SecretKeySerializer() {
        super(SecretKey.class);
    }

    @Override
    public void serialize(SecretKey value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStringField("algo", value.getAlgorithm());
        gen.writeBinaryField("key", value.getEncoded());
    }
}
