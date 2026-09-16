/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.kafka;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.kafka.common.serialization.Deserializer;

import io.leangen.geantyref.TypeFactory;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;

public class DeserializeKafka implements OpFactory<ByteBuffer, Object> {

    @SuppressWarnings("ImmutableEnumChecker")
    enum KafkaType {
        BOOLEAN(org.apache.kafka.common.serialization.BooleanDeserializer::new, Boolean.class),
        INTEGER(org.apache.kafka.common.serialization.IntegerDeserializer::new, Integer.class),
        LONG(org.apache.kafka.common.serialization.LongDeserializer::new, Long.class),
        FLOAT(org.apache.kafka.common.serialization.FloatDeserializer::new, Float.class),
        DOUBLE(org.apache.kafka.common.serialization.DoubleDeserializer::new, Double.class),
        BYTE_ARRAY(org.apache.kafka.common.serialization.ByteArrayDeserializer::new, byte[].class),
        STRING(org.apache.kafka.common.serialization.StringDeserializer::new, String.class),
        UUID(org.apache.kafka.common.serialization.UUIDDeserializer::new, UUID.class);
        //LIST(org.apache.kafka.common.serialization.ListDeserializer::new, TypeFactory.parameterizedClass(List.class, TypeFactory.unboundWildcard()));
        private final Supplier<Deserializer<?>> supplier;
        private final Type type;

        KafkaType(Supplier<Deserializer<?>> supplier, Type type) {
            this.supplier = supplier;
            this.type = type;
        }
        public Deserializer<?> deserializer() {
            return supplier.get();
        }
    }
    record Config(
            KafkaType kafkaType,
            Map<String, ?> deserializerConfigs,
            boolean isKey
    ) {}
    @Override
    public BaseTypedOp<ByteBuffer, Object> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        Config config1 = null;
        // TODO A Kafka deserialize is Closable, but the OpFactory doesn't allow to honour that contract
        Deserializer<?> deserializer = config1.kafkaType().deserializer();
        deserializer.configure(config1.deserializerConfigs(), config1.isKey());
        return BaseTypedOp.of(ByteBuffer.class, config1.kafkaType().type,
                (buffer, op) -> {
                    return deserializer.deserialize(
                            null /*TODO op.topic()*/,
                            null /* TODO op.headers()*/,
                            buffer);
                });
    }
}
