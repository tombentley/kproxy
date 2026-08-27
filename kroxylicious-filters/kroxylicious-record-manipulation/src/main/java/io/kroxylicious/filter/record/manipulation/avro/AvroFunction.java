/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.ContextPipeline;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.Requirement;
import io.kroxylicious.filter.record.manipulation.common.TypedOp;
import io.kroxylicious.filter.record.manipulation.config.OpConfig;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;

import edu.umd.cs.findbugs.annotations.NonNull;

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
            case STRING, INT, LONG, FLOAT, DOUBLE, BOOLEAN, BYTES -> (value, context) -> value;
            default -> throw new IllegalArgumentException("Avro mask not yet supported for schema type: " + schema.getType());
        };
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castToList(Object value) {
        return (List<Object>) value;
    }

    /**
     * Copies a {@link ByteBuffer}'s remaining bytes into a fresh array, without disturbing the buffer's own
     * position - {@link GenericRecord} fields declared {@code bytes} always read back as a {@link ByteBuffer},
     * never a {@code byte[]}.
     */
    private static byte[] toByteArray(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
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
            case BOOLEAN -> {
                ContextPipeline<Boolean, Boolean> pipeline = contextPipeline(ops, requirements, lookup, (op, pluginLookup) -> buildOp(op, Boolean.class, Boolean.class, pluginLookup));
                yield (value, context) -> pipeline.apply((Boolean) value, context);
            }
            case INT -> {
                ContextPipeline<Integer, Integer> pipeline = contextPipeline(ops, requirements, lookup, (op, pluginLookup) -> buildOp(op, Integer.class, Integer.class, pluginLookup));
                yield (value, context) -> pipeline.apply((Integer) value, context);
            }
            case LONG -> {
                ContextPipeline<Long, Long> pipeline = contextPipeline(ops, requirements, lookup, (op, pluginLookup) -> buildOp(op, Long.class, Long.class, pluginLookup));
                yield (value, context) -> pipeline.apply((Long) value, context);
            }
            case FLOAT -> {
                ContextPipeline<Float, Float> pipeline = contextPipeline(ops, requirements, lookup, (op, pluginLookup) -> buildOp(op, Float.class, Float.class, pluginLookup));
                yield (value, context) -> pipeline.apply((Float) value, context);
            }
            case DOUBLE -> {
                ContextPipeline<Double, Double> pipeline = contextPipeline(ops, requirements, lookup, (op, pluginLookup) -> buildOp(op, Double.class, Double.class, pluginLookup));
                yield (value, context) -> pipeline.apply((Double) value, context);
            }
            case STRING -> {
                ContextPipeline<String, String> pipeline = contextPipeline(ops, requirements, lookup, (op, pluginLookup) -> buildOp(op, String.class, String.class, pluginLookup));
                yield (value, context) -> {
                    // call toString() because value could be a Utf8, not a String
                    String input = value == null ? null : value.toString();
                    return pipeline.apply(input, context);
                };
            }
            case BYTES -> {
                ContextPipeline<byte[], byte[]> pipeline = contextPipeline(ops, requirements, lookup, (op, pluginLookup) -> buildOp(op, byte[].class, byte[].class, pluginLookup));
                yield (value, context) -> {
                    // call toByteArray() because value is a ByteBuffer, not a byte[]
                    byte[] input = value == null ? null : toByteArray((ByteBuffer) value);
                    byte[] result = pipeline.apply(input, context);
                    return result == null ? null : ByteBuffer.wrap(result);
                };
            }
            case FIXED -> {
                ContextPipeline<byte[], byte[]> pipeline = contextPipeline(ops, requirements, lookup, (op, pluginLookup) -> buildOp(op, byte[].class, byte[].class, pluginLookup));
                yield (value, context) -> {
                    GenericData.Fixed fixed = (GenericData.Fixed) value;
                    Schema schema = fixed.getSchema();
                    int fixedSize = schema.getFixedSize();
                    byte[] result = pipeline.apply(fixed.bytes(), context);
                    if (result == null) {
                        return null;
                    }
                    if (result.length != fixedSize) {
                        throw new IllegalArgumentException("Fixed type '" + schema.getName() + "' requires size " + fixedSize + " bytes but transformation resulted in " + result.length + " bytes");
                    }
                    return new GenericData.Fixed(schema, result);
                };
            }
            case ENUM -> {
                ContextPipeline<String, String> pipeline = contextPipeline(ops, requirements, lookup, (op, pluginLookup) -> buildOp(op, String.class, String.class, pluginLookup));
                yield (value, context) -> {
                    GenericData.EnumSymbol enumSymbol = (GenericData.EnumSymbol) value;
                    Schema schema = enumSymbol.getSchema();
                    var allowedSymbols = schema.getEnumSymbols();
                    String result = pipeline.apply(enumSymbol.toString(), context);
                    if (result == null) {
                        return null;
                    }
                    if (!allowedSymbols.contains(result)) {
                        throw new IllegalArgumentException("Enum type '" + schema.getName() + "' requires symbol in " + allowedSymbols + " but transformation resulted in '" + result + "'");
                    }
                    return new GenericData.EnumSymbol(schema, result);
                };
            }
            default -> throw new IllegalArgumentException("apply is not yet supported for type " + type);
        };
    }

    @NonNull
    private static <T, R> ContextPipeline<T, R> contextPipeline(List<OpConfig> ops,
                                                                Set<Requirement> requirements,
                                                                PluginLookup lookup,
                                                                BiFunction<OpConfig, PluginLookup, TypedOp<T, R>> fnn) {
        List<TypedOp<?, ?>> fns = ops.stream().<TypedOp<?, ?>> map(op -> fnn.apply(op, lookup)).toList();
        return new ContextPipeline<>(fns, requirements);
    }

    /**
     * Resolves one {@code apply} entry to a {@link TypedOp} - {@link OpConfigs#DELETE} is special-cased
     * here (rather than resolved via {@code lookup}) since removing a required Avro field isn't meaningful
     * without also supporting Avro's union/default mechanism (see the class javadoc), so it fails loudly
     * rather than producing a {@link GenericRecord} that no longer conforms to its schema - unlike
     * {@code JacksonFunction}'s equivalent, which allows it.
     */
    private static <T, R> TypedOp<T, R> buildOp(OpConfig op, Class<T> inputType, Class<R> outputType, PluginLookup lookup) {
        if (OpConfigs.DELETE.equals(op.op())) {
            throw new IllegalArgumentException("delete is not yet supported for Avro fields");
        }
        return OpConfigs.resolveOp(op, inputType, outputType, lookup);
    }
}
