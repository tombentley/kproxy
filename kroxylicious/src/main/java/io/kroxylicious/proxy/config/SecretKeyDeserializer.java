/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config;

import java.io.IOException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ValueNode;

class SecretKeyDeserializer extends StdDeserializer<SecretKey> {
    private final ObjectMapper MAPPER = new ObjectMapper();

    protected SecretKeyDeserializer() {
        super(SecretKey.class);
    }

    @Override
    public SecretKey deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        var tree = p.getCodec().readTree(p);
        var algoNode = tree.get("algo");
        var keyNode = tree.get("key");
        if (algoNode == null || !algoNode.isValueNode()) {
            throw new IllegalArgumentException("algo field is absent or is not text");
        }
        if (keyNode == null || !keyNode.isValueNode()) {
            throw new IllegalArgumentException("key field is absent or is not a value");
        }
        var algo = ((ValueNode) algoNode).textValue();
        var key = ((ValueNode) keyNode).binaryValue();
        return new SecretKeySpec(key, algo);
    }
}
