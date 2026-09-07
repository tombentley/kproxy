/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.protobuf;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.common.ListElements;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.Requirement;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.format.avro.AvroFunction;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpConfig;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A mask/transform over a Protobuf generic value (a {@link DynamicMessage}, a {@link java.util.List} for a
 * repeated field, or a leaf value such as a {@link String}/{@link Integer}), built from a
 * {@link ParsedProtoSchema} - the Protobuf equivalent of
 * {@link AvroFunction}.
 * <p>
 * Unlike Avro's array/{@code items} schema, a Protobuf {@code repeated} field has no separate node to hang
 * a per-element {@code apply} chain off - repeated-ness and element type live on the one
 * {@link Descriptors.FieldDescriptor}. So an {@code apply} declared on a repeated field is deliberately
 * interpreted as per-element (masking each element the way Avro's array {@code items.apply} does), rather
 * than as a whole-list operation (which nothing in {@code common} implements anyway) - a Protobuf-specific
 * design choice forced by its schema shape, not an accident of mirroring Avro too literally.
 */
public class ProtobufFunction extends StaticTypedOp<Object, Object> {

    /**
     * Builds a mask function from a {@link ParsedProtoSchema}, with no additional requirement beyond each
     * field's {@code apply} chain composing.
     * @param schema the parsed schema, plus its {@code apply} chains
     * @param lookup resolves the plugin implementation named by each {@code apply} entry's {@code op}
     * @return a function transforming an input value according to {@code schema}, given a {@link OpContext}
     */
    static ProtobufFunction buildMask(ParsedProtoSchema schema, PluginLookup lookup) {
        return buildMask(schema, Set.of(), lookup);
    }

    /**
     * Builds a mask function from a {@link ParsedProtoSchema}.
     * @param schema the parsed schema, plus its {@code apply} chains
     * @param requirements properties every field's composed {@code apply} chain must satisfy
     * @param lookup resolves the plugin implementation named by each {@code apply} entry's {@code op}
     * @return a function transforming an input value according to {@code schema}, given a {@link OpContext}
     */
    static ProtobufFunction buildMask(ParsedProtoSchema schema, Set<Requirement> requirements, PluginLookup lookup) {
        return buildMask(schema.descriptor(), schema.apply(), requirements, lookup);
    }

    private static ProtobufFunction buildMask(Descriptors.Descriptor descriptor, Map<Descriptors.GenericDescriptor, List<OpConfig>> applyByNode,
                                              Set<Requirement> requirements, PluginLookup lookup) {
        ProtobufFunction structural = buildStructural(descriptor, applyByNode, requirements, lookup);
        if (applyByNode.get(descriptor) != null) {
            throw new IllegalArgumentException("apply is not yet supported for message-level nodes: " + descriptor.getFullName());
        }
        return structural;
    }

    // /**
    // * Binds a fixed {@link OpContext} to this function, producing a plain {@code Function<Object,Object>}.
    // * Kept as loosely typed as the function itself since a mask can be built from a non-message schema too
    // * (e.g. one describing just a repeated field or a leaf) - see {@link #bindRecord(OpContext)} for the
    // * common case of composing a whole-message mask into a
    // * {@link Pipeline} between a deserializer and
    // * serializer, which are both typed to {@link DynamicMessage}.
    // * @param opContext the context to bind
    // * @return an equivalent {@code Function<Object,Object>}
    // */
    // default Function<Object, Object> bind(OpContext opContext) {
    // return new BoundProtoFunction(this, opContext);
    // }
    //
    // /**
    // * Binds a fixed {@link OpContext} to this function, producing a {@code Function<DynamicMessage,DynamicMessage>}
    // * suitable for composing into a whole-message {@link Pipeline}
    // * stage alongside {@link ProtobufBinaryDeserializer}/{@link ProtobufBinarySerializer}, which are themselves
    // * typed to {@link DynamicMessage} rather than {@code Object} - narrower than {@link #bind(OpContext)}
    // * purely so {@link Pipeline}'s reflection-based
    // * composition check lines up either side of this stage, not because the underlying transformation
    // * differs.
    // * @param opContext the context to bind
    // * @return an equivalent {@code Function<DynamicMessage,DynamicMessage>}
    // * @throws ClassCastException if this function was built from a non-message schema
    // */
    // default Function<DynamicMessage, DynamicMessage> bindRecord(OpContext opContext) {
    // return new BoundProtoMessageFunction(this, opContext);
    // }

    /**
     * A concrete (non-lambda) {@link Function} binding a fixed {@link OpContext} to a {@link ProtobufFunction} -
     * see {@code AvroFunction.BoundAvroFunction} for why this needs to be a named class.
     * @param fn the function being bound
     * @param opContext the context bound to it
     */
    record BoundProtoFunction(ProtobufFunction fn, OpContext opContext) implements Function<Object, Object> {
        @Override
        public Object apply(Object value) {
            return fn.apply(value, opContext);
        }
    }
    //
    // /**
    // * The {@link DynamicMessage}-typed counterpart of {@link BoundProtoFunction}, for the same reason - see
    // * {@link #bindRecord(OpContext)}.
    // * @param fn the function being bound
    // * @param opContext the context bound to it
    // */
    // record BoundProtoMessageFunction(ProtobufFunction fn, OpContext opContext) implements Function<DynamicMessage, DynamicMessage> {
    // @Override
    // public DynamicMessage apply(DynamicMessage value) {
    // return (DynamicMessage) fn.apply(value, opContext);
    // }
    // }

    /**
     * Builds the part of the mask that recurses into a message's declared fields, leaving leaves
     * untouched. This runs before each field's own {@code apply} chain (if any), so {@code apply} always
     * sees the already-masked value - mirrors {@code AvroFunction.buildStructural}.
     */
    private static ProtobufFunction buildStructural(Descriptors.Descriptor descriptor,
                                                    Map<Descriptors.GenericDescriptor, List<OpConfig>> applyByNode,
                                                    Set<Requirement> requirements,
                                                    PluginLookup lookup) {
        Map<String, BaseTypedOp<Object, Object>> mapping = descriptor.getFields().stream()
                .collect(Collectors.toMap(Descriptors.FieldDescriptor::getName, field -> fieldFunction(field, applyByNode, requirements, lookup), (a, b) -> a,
                        LinkedHashMap::new));
        var fn = ProtobufMessages.mapFields(descriptor, mapping);
        return new ProtobufFunction((value, context) -> fn.apply((DynamicMessage) value, context));
    }

    /**
     * Builds a field's mapping function: the field's own type-driven mask (recursing into a nested message
     * type), composed with any {@code apply} declared on the field itself, then - if the field is
     * {@code repeated} - lifted to run per-element (see the class javadoc for why {@code apply} is
     * per-element rather than whole-list here).
     */
    private static ProtobufFunction fieldFunction(Descriptors.FieldDescriptor field,
                                                  Map<Descriptors.GenericDescriptor, List<OpConfig>> applyByNode,
                                                  Set<Requirement> requirements, PluginLookup lookup) {
        if (field.isMapField()) {
            throw new IllegalArgumentException("Proto mask not yet supported for map fields: " + field.getFullName());
        }
        ProtobufFunction elementFn = buildElementTypeMask(field, applyByNode, requirements, lookup);
        List<OpConfig> apply = applyByNode.get(field);
        if (apply != null) {
            ProtobufFunction ownApply = buildApplyChain(field, apply, requirements, lookup);
            ProtobufFunction base = elementFn;
            elementFn = new ProtobufFunction((value, context) -> ownApply.apply(base.apply(value, context), context));
        }
        if (field.isRepeated()) {
            ProtobufFunction perElement = elementFn;
            return new ProtobufFunction((value, context) -> new ListElements().modifyAll(castToList(value), perElement, context));
        }
        return elementFn;
    }

    /**
     * Builds the mask for one element of {@code field} (its message type, if any - recursing via
     * {@link #buildMask(Descriptors.Descriptor, Map, Set, PluginLookup)} - or a passthrough for a supported
     * scalar leaf type), ignoring {@code field}'s repeated-ness, which {@link #fieldFunction} applies
     * separately.
     */
    private static ProtobufFunction buildElementTypeMask(Descriptors.FieldDescriptor field,
                                                         Map<Descriptors.GenericDescriptor, List<OpConfig>> applyByNode,
                                                         Set<Requirement> requirements,
                                                         PluginLookup lookup) {
        return switch (field.getType()) {
            case MESSAGE -> buildMask(field.getMessageType(), applyByNode, requirements, lookup);
            case STRING, INT32, INT64, FLOAT, DOUBLE, BOOL, BYTES -> new ProtobufFunction((value, context) -> value);
            default -> throw new IllegalArgumentException("Proto mask not yet supported for field type: " + field.getType());
        };
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castToList(Object value) {
        return (List<Object>) value;
    }

    private static ProtobufFunction buildApplyChain(Descriptors.FieldDescriptor field, List<OpConfig> ops, Set<Requirement> requirements, PluginLookup lookup) {
        return switch (field.getType()) {
            case BOOL -> {
                var pipeline = OpConfigs.compose(Boolean.class, ops, requirements, lookup);
                yield new ProtobufFunction((value, context) -> pipeline.apply((Boolean) value, context));
            }
            case INT32, FIXED32, SINT32, SFIXED32, UINT32 -> {
                var pipeline = OpConfigs.compose(Integer.class, ops, requirements, lookup);
                yield new ProtobufFunction((value, context) -> pipeline.apply((Integer) value, context));
            }
            case INT64, FIXED64, SINT64, SFIXED64, UINT64 -> {
                var pipeline = OpConfigs.compose(Long.class, ops, requirements, lookup);
                yield new ProtobufFunction((value, context) -> pipeline.apply((Long) value, context));
            }
            case FLOAT -> {
                var pipeline = OpConfigs.compose(Float.class, ops, requirements, lookup);
                yield new ProtobufFunction((value, context) -> pipeline.apply((Float) value, context));
            }
            case DOUBLE -> {
                var pipeline = OpConfigs.compose(Double.class, ops, requirements, lookup);
                yield new ProtobufFunction((value, context) -> pipeline.apply((Double) value, context));
            }
            case STRING -> {
                var pipeline = OpConfigs.compose(String.class, ops, requirements, lookup);
                yield new ProtobufFunction((value, context) -> pipeline.apply(value == null ? null : value.toString(), context));
            }
            case BYTES -> {
                var pipeline = OpConfigs.compose(byte[].class, ops, requirements, lookup);
                yield new ProtobufFunction((value, context) -> {
                    // call toByteArray() because value is a ByteString, not a byte[]
                    byte[] input = value == null ? null : ((ByteString) value).toByteArray();
                    byte[] result = (byte[]) pipeline.apply(input, context);
                    return result == null ? null : ByteString.copyFrom(result);
                });
            }
            case ENUM -> {
                var pipeline = OpConfigs.compose(String.class, ops, requirements, lookup);
                yield new ProtobufFunction((value, context) -> {
                    Enum<?> enumSymbol = (Enum<?>) value;
                    Class<? extends Enum> enumClass = enumSymbol.getClass();
                    var allowedSymbols = Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).collect(Collectors.toSet());
                    String result = (String) pipeline.apply(enumSymbol.name(), context);
                    if (result == null) {
                        return null;
                    }
                    if (!allowedSymbols.contains(result)) {
                        throw new IllegalArgumentException(
                                "Enum type '" + enumClass.getName() + "' requires symbol in " + allowedSymbols + " but transformation resulted in '" + result + "'");
                    }
                    return Enum.valueOf(enumClass, result);
                });
            }
            default -> throw new IllegalArgumentException("apply is not yet supported for field type: " + field.getType());
        };
    }

    // @NonNull
    // private static <T, R> TypedOp<T, R> contextPipeline(List<OpConfig> ops,
    // Set<Requirement> requirements,
    // PluginLookup lookup,
    // BiFunction<OpConfig, PluginLookup, TypedOp<T, R>> factoryFn) {
    // List<TypedOp<?, ?>> fns = ops.stream().<TypedOp<?, ?>> map(op -> factoryFn.apply(op, lookup)).toList();
    // return new OpPipeline<>(fns, requirements);
    // }

    // /**
    // * Resolves one {@code apply} entry to a {@link TypedOp} - {@link OpConfigs#DELETE} is special-cased
    // * here (rather than resolved via {@code lookup}) since removing a field isn't meaningful without
    // * deciding what it means for a required proto2/proto3 implicit-presence field, so it fails loudly
    // * rather than producing a possibly-nonconforming message - unlike {@code JacksonFunction}'s equivalent,
    // * which allows it.
    // */
    // static <T, R> TypedOp<T, R> buildOp(OpConfig op, Class<T> inputType, Class<R> outputType, PluginLookup lookup) {
    // if (OpConfigs.DELETE.equals(op.op())) {
    // throw new IllegalArgumentException("delete is not yet supported for Protobuf fields");
    // }
    // return OpConfigs.resolveOp(op, inputType, outputType, lookup);
    // }

    private final BiFunction<Object, OpContext, Object> fn;

    public ProtobufFunction(BiFunction<Object, OpContext, Object> fn) {
        this.fn = fn;
    }

    @Override
    public Type outputType(Type inputType) {
        return Object.class;
    }

    @Override
    public Object apply(Object value, OpContext opContext) {
        return fn.apply(value, opContext);
    }
}
