/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.avro.Schema;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Masks/transforms an {@link AvroValue} per its bundled schema's {@code apply} annotations (see
 * {@link AvroSchemas}) - the Avro equivalent of
 * {@link io.kroxylicious.filter.record.manipulation.format.jackson.JsonTransform}.
 * <p>
 * Takes no schema config of its own - see {@link AvroValue} for why it instead builds its mask from
 * whatever {@link Schema} arrives bundled with each value. {@link AvroFunction} itself is deliberately
 * loosely typed ({@code Object}-to-{@code Object}), since a mask can be built from a non-record schema too
 * (an array or a leaf) - this op doesn't scope itself to record-shaped schemas, so array/scalar/enum/fixed
 * roots work here just as well as records.
 * <p>
 * The built {@link AvroFunction} is cached against the {@link Schema} it was built from (compared with
 * {@link Schema#equals(Object)}, which is structural - unlike {@code ProtobufTransform}'s equivalent cache,
 * no reference-identity trick is needed here): a {@link DeserializeAvro}-configured op hands out an
 * {@code .equals()} schema on every call, so this cache hits on every message after the first, avoiding
 * rebuilding the whole mask in what is otherwise a per-record hot path.
 */
@Plugin(configType = Void.class)
public class AvroTransform implements OpFactory<AvroValue, AvroValue> {

    @Override
    public BaseTypedOp<AvroValue, AvroValue> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        return BaseTypedOp.of(AvroValue.class, AvroValue.class, new CachingMask(lookup));
    }

    private static final class CachingMask implements BiFunction<AvroValue, OpContext, AvroValue> {
        private final PluginLookup lookup;
        private volatile Schema cachedSchema;
        private volatile AvroFunction cachedMask;

        private CachingMask(PluginLookup lookup) {
            this.lookup = lookup;
        }

        @Override
        public AvroValue apply(AvroValue value, OpContext opContext) {
            Schema schema = value.schema();
            AvroFunction mask = cachedMask;
            if (mask == null || !schema.equals(cachedSchema)) {
                mask = AvroFunction.buildMask(schema, lookup);
                cachedSchema = schema;
                cachedMask = mask;
            }
            Object masked = mask.apply(value.value(), opContext);
            return new AvroValue(masked, schema);
        }
    }
}
