/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.format.protobuf;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.protobuf.DynamicMessage;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.transform.neo.format.avro.AvroTransform;
import io.kroxylicious.transform.neo.op.BaseTypedOp;
import io.kroxylicious.transform.neo.op.OpContext;
import io.kroxylicious.transform.neo.op.OpFactory;
import io.kroxylicious.transform.service.TransformInitializationContext;

/**
 * Masks/transforms a {@link ProtoValue} per its bundled withKeySchema's {@code apply} annotations (see
 * {@link ProtobufSchemaParser}) - the Protobuf equivalent of
 * {@link AvroTransform}.
 * <p>
 * Takes no withKeySchema config of its own - see {@link ProtoValue} for why it instead builds its mask from
 * whatever {@link ParsedProtoSchema} arrives bundled with each value. The built {@link ProtobufFunction}
 * is cached against the {@link ParsedProtoSchema} instance it was built from (compared by reference):
 * a {@link DeserializeProtobuf}-configured op returns the same withKeySchema instance on every call, so this
 * cache hits on every message after the first, avoiding rebuilding the whole mask in what is otherwise a
 * per-record hot path.
 */
@Plugin(configType = Void.class)
public class ProtobufTransform implements OpFactory<ProtoValue, ProtoValue> {

    @Override
    public BaseTypedOp<ProtoValue, ProtoValue> create(Map<String, Object> config, TransformInitializationContext lookup, Type argumentType) {
        return BaseTypedOp.of(ProtoValue.class, ProtoValue.class, new CachingMask(lookup));
    }

    private static final class CachingMask implements java.util.function.BiFunction<ProtoValue, OpContext, ProtoValue> {
        private final TransformInitializationContext lookup;
        private volatile ParsedProtoSchema cachedSchema;
        private volatile ProtobufFunction cachedMask;

        private CachingMask(TransformInitializationContext lookup) {
            this.lookup = lookup;
        }

        @Override
        @SuppressWarnings("ReferenceEquality") // deliberate identity check: a withKeySchema instance is reused verbatim across calls, see class javadoc
        public ProtoValue apply(ProtoValue value, OpContext opContext) {
            ParsedProtoSchema schema = value.schema();
            ProtobufFunction mask = cachedMask;
            if (mask == null || cachedSchema != schema) {
                mask = ProtobufFunction.buildMask(schema, lookup);
                cachedSchema = schema;
                cachedMask = mask;
            }
            DynamicMessage masked = (DynamicMessage) mask.apply(value.message(), opContext);
            return new ProtoValue(masked, schema);
        }
    }
}
