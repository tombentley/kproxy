/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.ContextPipeline;
import io.kroxylicious.filter.record.manipulation.common.IntOp;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.Requirement;
import io.kroxylicious.filter.record.manipulation.common.StringOp;
import io.kroxylicious.filter.record.manipulation.config.OpConfig;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;

/**
 * A mask/transform over an Avro generic value (a {@link GenericRecord}, a {@link java.util.List} for an
 * array, or a leaf value such as a {@link String}/{@link Integer}), built from a {@link Schema} annotated
 * with the non-standard {@code apply} keyword (see {@link AvroSchemas}) - the Avro equivalent of
 * {@link io.kroxylicious.filter.record.manipulation.jackson.JacksonFunction}.
 * <p>
 * Unlike {@code JacksonFunction}, this only masks a value that already conforms to {@code schema}; it has
 * no generation-from-nothing mode, since Avro requires every declared field to be present, so there's no
 * "absent" starting point equivalent to Jackson's {@link com.fasterxml.jackson.databind.node.MissingNode}
 * to generate from yet (that needs Avro's union/default mechanism first).
 * <p>
 * Declared as its own interface (rather than using {@code BiFunction<Object, Context, Object>} directly)
 * for the same reason as {@code JacksonFunction} - see its javadoc for why {@link ContextPipeline} needs
 * a fixed, reflectable generic signature.
 */
public interface AvroFunction extends BiFunction<Object, Context, Object> {

    /**
     * Builds a mask function from a {@link Schema} tree, with no additional requirement beyond each
     * field's {@code apply} chain composing.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @param lookup resolves the plugin implementation named by each {@code apply} entry's {@code op}
     * @return a function transforming an input value according to {@code schema}, given a {@link Context}
     */
    static AvroFunction buildMask(Schema schema, PluginLookup lookup) {
        return buildMask(schema, Set.of(), lookup);
    }

    /**
     * Builds a mask function from a {@link Schema} tree.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @param requirements properties every field's composed {@code apply} chain must satisfy
     * @param lookup resolves the plugin implementation named by each {@code apply} entry's {@code op}
     * @return a function transforming an input value according to {@code schema}, given a {@link Context}
     */
    static AvroFunction buildMask(Schema schema, Set<Requirement> requirements, PluginLookup lookup) {
        AvroFunction structural = buildStructural(schema, requirements, lookup);
        List<OpConfig> apply = AvroSchemas.applyConfig(schema);
        if (apply == null) {
            return structural;
        }
        AvroFunction ownApply = buildApplyChain(schema.getType(), apply, requirements, lookup);
        return (value, context) -> ownApply.apply(structural.apply(value, context), context);
    }

    /**
     * Binds a fixed {@link Context} to this function, producing a plain {@code Function<Object,Object>}.
     * Kept as loosely typed as the function itself since a mask can be built from a non-record schema too
     * (e.g. one describing just an array or a leaf) - see {@link #bindRecord(Context)} for the common case
     * of composing a whole-record mask into a {@link io.kroxylicious.filter.record.manipulation.common.Pipeline}
     * between a deserializer and serializer, which are both typed to {@link GenericRecord}.
     * @param context the context to bind
     * @return an equivalent {@code Function<Object,Object>}
     */
    default Function<Object, Object> bind(Context context) {
        return new BoundAvroFunction(this, context);
    }

    /**
     * Binds a fixed {@link Context} to this function, producing a {@code Function<GenericRecord,GenericRecord>}
     * suitable for composing into a whole-record {@link io.kroxylicious.filter.record.manipulation.common.Pipeline}
     * stage alongside {@link AvroBinaryDeserializer}/{@link AvroBinarySerializer} (or the JSON equivalents),
     * which are themselves typed to {@link GenericRecord} rather than {@code Object} - narrower than
     * {@link #bind(Context)} purely so {@link io.kroxylicious.filter.record.manipulation.common.Pipeline}'s
     * reflection-based composition check (each stage's return type must be assignable to the next stage's
     * parameter type) lines up either side of this stage, not because the underlying transformation differs.
     * @param context the context to bind
     * @return an equivalent {@code Function<GenericRecord,GenericRecord>}
     * @throws ClassCastException if this function was built from a non-record schema
     */
    default Function<GenericRecord, GenericRecord> bindRecord(Context context) {
        return new BoundAvroRecordFunction(this, context);
    }

    /**
     * A concrete (non-lambda) {@link Function} binding a fixed {@link Context} to an {@link AvroFunction} -
     * see {@code JacksonFunction.BoundJacksonFunction} for why this needs to be a named class.
     * @param fn the function being bound
     * @param context the context bound to it
     */
    record BoundAvroFunction(AvroFunction fn, Context context) implements Function<Object, Object> {
        @Override
        public Object apply(Object value) {
            return fn.apply(value, context);
        }
    }

    /**
     * The {@link GenericRecord}-typed counterpart of {@link BoundAvroFunction}, for the same reason - see
     * {@link #bindRecord(Context)}.
     * @param fn the function being bound
     * @param context the context bound to it
     */
    record BoundAvroRecordFunction(AvroFunction fn, Context context) implements Function<GenericRecord, GenericRecord> {
        @Override
        public GenericRecord apply(GenericRecord value) {
            return (GenericRecord) fn.apply(value, context);
        }
    }

    /**
     * Builds the part of the mask that recurses into a schema's declared children ({@code fields}/
     * {@code items}), leaving leaves untouched. This runs before the schema's own {@code apply} chain (if
     * any), so {@code apply} always sees the already-masked children - mirrors
     * {@code JacksonFunction.buildStructural}.
     */
    private static AvroFunction buildStructural(Schema schema, Set<Requirement> requirements, PluginLookup lookup) {
        return switch (schema.getType()) {
            case RECORD -> {
                Map<String, BiFunction<Object, Context, Object>> mapping = schema.getFields().stream()
                        .collect(Collectors.toMap(Schema.Field::name, field -> fieldFunction(field, requirements, lookup), (a, b) -> a, LinkedHashMap::new));
                var fn = AvroRecords.mapFields(schema, mapping);
                yield (value, context) -> fn.apply((GenericRecord) value, context);
            }
            case ARRAY -> {
                var fn = AvroArrays.items(buildMask(schema.getElementType(), requirements, lookup));
                yield (value, context) -> fn.apply(castToList(value), context);
            }
            case STRING, INT -> (value, context) -> value;
            default -> throw new IllegalArgumentException("Avro mask not yet supported for schema type: " + schema.getType());
        };
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castToList(Object value) {
        return (List<Object>) value;
    }

    /**
     * Builds a record field's mapping function: the field's own type-driven mask (which may itself carry
     * a schema-level {@code apply}, e.g. on an array's {@code items}), composed with any {@code apply}
     * declared directly on the field (a sibling of the field's {@code type}, per {@link AvroSchemas}).
     */
    private static BiFunction<Object, Context, Object> fieldFunction(Schema.Field field, Set<Requirement> requirements, PluginLookup lookup) {
        AvroFunction base = buildMask(field.schema(), requirements, lookup);
        List<OpConfig> apply = AvroSchemas.applyConfig(field);
        if (apply == null) {
            return base;
        }
        AvroFunction ownApply = buildApplyChain(field.schema().getType(), apply, requirements, lookup);
        return (value, context) -> ownApply.apply(base.apply(value, context), context);
    }

    /**
     * Builds and composes a node's own {@code apply} list into a single function, via {@link ContextPipeline} -
     * mirrors {@code JacksonFunction.buildApplyChain}, minus {@code delete}: removing a required Avro field
     * isn't meaningful without also supporting Avro's union/default mechanism (see the class javadoc), so
     * it fails loudly rather than producing a {@link GenericRecord} that no longer conforms to its schema.
     */
    private static AvroFunction buildApplyChain(Schema.Type type, List<OpConfig> ops, Set<Requirement> requirements, PluginLookup lookup) {
        return switch (type) {
            case STRING -> {
                List<BiFunction<?, Context, ?>> fns = ops.stream().<BiFunction<?, Context, ?>> map(op -> buildStringOp(op, lookup)).toList();
                ContextPipeline pipeline = new ContextPipeline(fns, requirements);
                yield (value, context) -> pipeline.<String, String> apply(value == null ? null : value.toString(), context);
            }
            case INT -> {
                List<BiFunction<?, Context, ?>> fns = ops.stream().<BiFunction<?, Context, ?>> map(op -> buildIntegerOp(op, lookup)).toList();
                ContextPipeline pipeline = new ContextPipeline(fns, requirements);
                yield (value, context) -> pipeline.<Integer, Integer> apply((Integer) value, context);
            }
            default -> throw new IllegalArgumentException("apply is not yet supported for type " + type);
        };
    }

    /**
     * Resolves one {@code apply} entry to a {@link StringOp} - {@link OpConfigs#DELETE} is special-cased
     * here (rather than resolved via {@code lookup}) since removing a required Avro field isn't meaningful
     * without also supporting Avro's union/default mechanism (see the class javadoc), so it fails loudly
     * rather than producing a {@link GenericRecord} that no longer conforms to its schema - unlike
     * {@code JacksonFunction}'s equivalent, which allows it.
     */
    private static StringOp buildStringOp(OpConfig op, PluginLookup lookup) {
        if (OpConfigs.DELETE.equals(op.op())) {
            throw new IllegalArgumentException("delete is not yet supported for Avro fields");
        }
        return OpConfigs.resolveStringOp(op, lookup);
    }

    /**
     * The {@link IntOp} counterpart of {@link #buildStringOp(OpConfig, PluginLookup)}.
     */
    private static IntOp buildIntegerOp(OpConfig op, PluginLookup lookup) {
        if (OpConfigs.DELETE.equals(op.op())) {
            throw new IllegalArgumentException("delete is not yet supported for Avro fields");
        }
        return OpConfigs.resolveIntOp(op, lookup);
    }
}
