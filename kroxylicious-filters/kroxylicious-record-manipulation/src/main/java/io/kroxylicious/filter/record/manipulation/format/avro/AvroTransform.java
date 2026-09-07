/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.lang.reflect.Type;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.plugin.Plugin;

/**
 * Masks/transforms a {@link GenericRecord} per a {@link Schema} tree annotated with the non-standard
 * {@code apply} keyword (see {@link AvroSchemas}) - the Avro equivalent of
 * {@link io.kroxylicious.filter.record.manipulation.format.jackson.JsonTransform}.
 * <p>
 * {@link AvroFunction} itself is deliberately loosely typed ({@code Object}-to-{@code Object}), since a
 * mask can in principle be built from a non-record schema too (an array or a leaf). This op scopes itself
 * to record-shaped masks - the only shape {@link AvroBinaryDeserializer}/{@link AvroBinarySerializer}
 * (which this op composes between, via config) support anyway - so it can expose the precise
 * {@code GenericRecord}-to-{@code GenericRecord} type that composition needs, rather than falling back to
 * {@code Object}.
 */
@Plugin(configType = AvroTransform.Config.class)
public class AvroTransform implements OpFactory<GenericRecord, GenericRecord> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record Config(String schema) {}

    @Override
    public BaseTypedOp<GenericRecord, GenericRecord> create(Map<String, Object> config, PluginLookup lookup, Type argumentType) {
        Config c = MAPPER.convertValue(config, Config.class);
        Schema schema = new Schema.Parser().parse(c.schema());
        if (schema.getType() != Schema.Type.RECORD) {
            throw new IllegalArgumentException("AvroTransform requires a record schema, but got: " + schema.getType());
        }
        AvroFunction mask = AvroFunction.buildMask(schema, lookup);
        return new StaticTypedOp<GenericRecord, GenericRecord>() {
            @Override
            public Type outputType(Type inputType) {
                return GenericRecord.class;
            }

            @Override
            public GenericRecord apply(GenericRecord value, OpContext opContext) {
                return (GenericRecord) mask.apply(value, opContext);
            }
        };
    }
}
