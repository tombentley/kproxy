/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.avro;

import java.lang.reflect.Type;
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

import com.fasterxml.jackson.databind.JsonNode;

import io.leangen.geantyref.GenericTypeReflector;

import io.kroxylicious.filter.record.manipulation.common.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.common.OpContext;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.Requirement;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.filter.record.manipulation.common.TypeException;
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
 */
public class AvroFunction extends StaticTypedOp<Object, Object> {

    private final BiFunction<Object, OpContext, Object> fn;

    public AvroFunction(BiFunction<Object, OpContext, Object> fn) {
        this.fn = fn;
    }

    @Override
    public Type outputType(Type inputType) {
        if (!GenericTypeReflector.isSuperType(JsonNode.class, inputType)) {
            throw new TypeException("Input type " + GenericTypeReflector.getTypeName(inputType)
                    + " is not a subtype of " + GenericTypeReflector.getTypeName(JsonNode.class));
        }
        return JsonNode.class;
    }

    @Override
    public Object apply(Object value, OpContext opContext) {
        return fn.apply(value, opContext);
    }

    /**
     * Builds a mask function from a {@link Schema} tree, with no additional requirement beyond each
     * field's {@code apply} chain composing.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @param lookup resolves the plugin implementation named by each {@code apply} entry's {@code op}
     * @return a function transforming an input value according to {@code schema}, given a {@link OpContext}
     */
    static AvroFunction buildMask(Schema schema, PluginLookup lookup) {
        return buildMask(schema, Set.of(), lookup);
    }

    /**
     * Builds a mask function from a {@link Schema} tree.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @param requirements properties every field's composed {@code apply} chain must satisfy
     * @param lookup resolves the plugin implementation named by each {@code apply} entry's {@code op}
     * @return a function transforming an input value according to {@code schema}, given a {@link OpContext}
     */
    static AvroFunction buildMask(Schema schema, Set<Requirement> requirements, PluginLookup lookup) {
        AvroFunction structural = buildStructural(schema, requirements, lookup);
        List<OpConfig> apply = AvroSchemas.applyConfig(schema);
        AvroFunction foo;
        if (apply == null) {
            foo = structural;
        }
        else {
            AvroFunction ownApply = buildApplyChain(schema.getType(), apply, requirements, lookup);
            foo = new AvroFunction((value, context) -> ownApply.apply(structural.apply(value, context), context));
        }
        return foo;
    }

//    /**
//     * Binds a fixed {@link OpContext} to this function, producing a plain {@code Function<Object,Object>}.
//     * Kept as loosely typed as the function itself since a mask can be built from a non-record schema too
//     * (e.g. one describing just an array or a leaf) - see {@link #bindRecord(OpContext)} for the common case
//     * of composing a whole-record mask into a {@link Pipeline}
//     * between a deserializer and serializer, which are both typed to {@link GenericRecord}.
//     * @param opContext the context to bind
//     * @return an equivalent {@code Function<Object,Object>}
//     */
//    default Function<Object, Object> bind(OpContext opContext) {
//        return new BoundAvroFunction(this, opContext);
//    }
//
//    /**
//     * Binds a fixed {@link OpContext} to this function, producing a {@code Function<GenericRecord,GenericRecord>}
//     * suitable for composing into a whole-record {@link Pipeline}
//     * stage alongside {@link AvroBinaryDeserializer}/{@link AvroBinarySerializer} (or the JSON equivalents),
//     * which are themselves typed to {@link GenericRecord} rather than {@code Object} - narrower than
//     * {@link #bind(OpContext)} purely so {@link Pipeline}'s
//     * reflection-based composition check (each stage's return type must be assignable to the next stage's
//     * parameter type) lines up either side of this stage, not because the underlying transformation differs.
//     * @param opContext the context to bind
//     * @return an equivalent {@code Function<GenericRecord,GenericRecord>}
//     * @throws ClassCastException if this function was built from a non-record schema
//     */
//    default Function<GenericRecord, GenericRecord> bindRecord(OpContext opContext) {
//        return new BoundAvroRecordFunction(this, opContext);
//    }

    /**
     * A concrete (non-lambda) {@link Function} binding a fixed {@link OpContext} to an {@link AvroFunction} -
     * see {@code JacksonFunction.BoundJacksonFunction} for why this needs to be a named class.
     * @param fn the function being bound
     * @param opContext the context bound to it
     */
    record BoundAvroFunction(AvroFunction fn, OpContext opContext) implements Function<Object, Object> {
        @Override
        public Object apply(Object value) {
            return fn.apply(value, opContext);
        }
    }

//    /**
//     * The {@link GenericRecord}-typed counterpart of {@link BoundAvroFunction}, for the same reason - see
//     * {@link #bindRecord(OpContext)}.
//     * @param fn the function being bound
//     * @param opContext the context bound to it
//     */
//    record BoundAvroRecordFunction(AvroFunction fn, OpContext opContext) implements Function<GenericRecord, GenericRecord> {
//        @Override
//        public GenericRecord apply(GenericRecord value) {
//            return (GenericRecord) fn.apply(value, opContext);
//        }
//    }

    /**
     * Builds the part of the mask that recurses into a schema's declared children ({@code fields}/
     * {@code items}), leaving leaves untouched. This runs before the schema's own {@code apply} chain (if
     * any), so {@code apply} always sees the already-masked children - mirrors
     * {@code JacksonFunction.buildStructural}.
     */
    private static AvroFunction buildStructural(Schema schema, Set<Requirement> requirements, PluginLookup lookup) {
        return switch (schema.getType()) {
            case RECORD -> {
                Map<String, BaseTypedOp<Object, Object>> mapping = schema.getFields().stream()
                        .collect(Collectors.toMap(Schema.Field::name, field -> fieldFunction(field, requirements, lookup), (a, b) -> a, LinkedHashMap::new));
                var fn = AvroRecords.mapFields(schema, mapping);
                yield new AvroFunction((value, context) -> fn.apply((GenericRecord) value, context));
            }
            case ARRAY -> {
                var fn = AvroArrays.items(buildMask(schema.getElementType(), requirements, lookup));
                yield new AvroFunction((value, opContext) -> fn.apply(castToList(value), opContext));
            }
            case STRING, INT, LONG, FLOAT, DOUBLE, BOOLEAN, BYTES -> new AvroFunction((value, context) -> value);
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
    private static AvroFunction fieldFunction(Schema.Field field, Set<Requirement> requirements, PluginLookup lookup) {
        AvroFunction base = buildMask(field.schema(), requirements, lookup);
        List<OpConfig> apply = AvroSchemas.applyConfig(field);
        if (apply == null) {
            return base;
        }
        AvroFunction ownApply = buildApplyChain(field.schema().getType(), apply, requirements, lookup);
        return new AvroFunction((value, context) -> ownApply.apply(base.apply(value, context), context));
    }

    private static AvroFunction buildApplyChain(Schema.Type type, List<OpConfig> ops, Set<Requirement> requirements, PluginLookup lookup) {
        return switch (type) {
            case BOOLEAN -> {
                var pipeline = OpConfigs.compose(Boolean.class, ops, requirements, lookup);
                yield new AvroFunction((value, context) -> pipeline.apply((Boolean) value, context));
            }
            case INT -> {
                var pipeline = OpConfigs.compose(Integer.class, ops, requirements, lookup);
                yield new AvroFunction((value, context) -> pipeline.apply((Integer) value, context));
            }
            case LONG -> {
                var pipeline = OpConfigs.compose(Long.class, ops, requirements, lookup);
                yield new AvroFunction((value, context) -> pipeline.apply((Long) value, context));
            }
            case FLOAT -> {
                var pipeline = OpConfigs.compose(Float.class, ops, requirements, lookup);
                yield new AvroFunction((value, context) -> pipeline.apply((Float) value, context));
            }
            case DOUBLE -> {
                var pipeline = OpConfigs.compose(Double.class, ops, requirements, lookup);
                yield new AvroFunction((value, context) -> pipeline.apply((Double) value, context));
            }
            case STRING -> {
                var pipeline = OpConfigs.compose(String.class, ops, requirements, lookup);
                yield new AvroFunction((value, context) -> {
                    // call toString() because value could be a Utf8, not a String
                    String input = value == null ? null : value.toString();
                    return pipeline.apply(input, context);
                });
            }
            case BYTES -> {
                var pipeline = OpConfigs.compose(byte[].class, ops, requirements, lookup);
                yield new AvroFunction((value, context) -> {
                    // call toByteArray() because value is a ByteBuffer, not a byte[]
                    byte[] input = value == null ? null : toByteArray((ByteBuffer) value);
                    byte[] result = (byte[]) pipeline.apply(input, context);
                    return result == null ? null : ByteBuffer.wrap(result);
                });
            }
            case FIXED -> {
                var pipeline = OpConfigs.compose(byte[].class, ops, requirements, lookup);
                yield new AvroFunction((value, context) -> {
                    GenericData.Fixed fixed = (GenericData.Fixed) value;
                    Schema schema = fixed.getSchema();
                    int fixedSize = schema.getFixedSize();
                    byte[] result = (byte[]) pipeline.apply(fixed.bytes(), context);
                    if (result == null) {
                        return null;
                    }
                    if (result.length != fixedSize) {
                        throw new IllegalArgumentException("Fixed type '" + schema.getName() + "' requires size " + fixedSize + " bytes but transformation resulted in " + result.length + " bytes");
                    }
                    return new GenericData.Fixed(schema, result);
                });
            }
            case ENUM -> {
                var pipeline = OpConfigs.compose(String.class, ops, requirements, lookup);
                yield new AvroFunction((value, context) -> {
                    GenericData.EnumSymbol enumSymbol = (GenericData.EnumSymbol) value;
                    Schema schema = enumSymbol.getSchema();
                    var allowedSymbols = schema.getEnumSymbols();
                    String result = (String) pipeline.apply(enumSymbol.toString(), context);
                    if (result == null) {
                        return null;
                    }
                    if (!allowedSymbols.contains(result)) {
                        throw new IllegalArgumentException("Enum type '" + schema.getName() + "' requires symbol in " + allowedSymbols + " but transformation resulted in '" + result + "'");
                    }
                    return new GenericData.EnumSymbol(schema, result);
                });
            }
            default -> throw new IllegalArgumentException("apply is not yet supported for type " + type);
        };
    }

//    @NonNull
//    private static <T, R> OpPipeline<T, R> contextPipeline(List<OpConfig> ops,
//                                                           Set<Requirement> requirements,
//                                                           PluginLookup lookup,
//                                                           BiFunction<OpConfig, PluginLookup, TypedOp<T, R>> factoryFn) {
//        List<TypedOp<?, ?>> fns = ops.stream().<TypedOp<?, ?>> map(op -> factoryFn.apply(op, lookup)).toList();
//        return new OpPipeline<>(fns, requirements);
//    }
//
//    /**
//     * Resolves one {@code apply} entry to a {@link TypedOp} - {@link OpConfigs#DELETE} is special-cased
//     * here (rather than resolved via {@code lookup}) since removing a required Avro field isn't meaningful
//     * without also supporting Avro's union/default mechanism (see the class javadoc), so it fails loudly
//     * rather than producing a {@link GenericRecord} that no longer conforms to its schema - unlike
//     * {@code JacksonFunction}'s equivalent, which allows it.
//     */
//    private static <T, R> TypedOp<T, R> buildOp(OpConfig op, Class<T> inputType, Class<R> outputType, PluginLookup lookup) {
//        if (OpConfigs.DELETE.equals(op.op())) {
//            throw new IllegalArgumentException("delete is not yet supported for Avro fields");
//        }
//        return OpConfigs.resolveOp(op, inputType, outputType, lookup);
//    }
}
