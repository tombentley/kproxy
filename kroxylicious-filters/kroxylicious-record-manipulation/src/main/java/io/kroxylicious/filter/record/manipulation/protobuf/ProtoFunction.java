/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.protobuf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.common.ChooseIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.ChooseStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.ContextPipeline;
import io.kroxylicious.filter.record.manipulation.common.DecryptStringFunction;
import io.kroxylicious.filter.record.manipulation.common.EncryptStringFunction;
import io.kroxylicious.filter.record.manipulation.common.HmacStringFunction;
import io.kroxylicious.filter.record.manipulation.common.IntOp;
import io.kroxylicious.filter.record.manipulation.common.IntOpFactory;
import io.kroxylicious.filter.record.manipulation.common.ListElements;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.RandomIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.RandomStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.Requirement;
import io.kroxylicious.filter.record.manipulation.common.StringOp;
import io.kroxylicious.filter.record.manipulation.config.ApplyConfig;
import io.kroxylicious.filter.record.manipulation.config.OpConfig;

/**
 * A mask/transform over a Protobuf generic value (a {@link DynamicMessage}, a {@link java.util.List} for a
 * repeated field, or a leaf value such as a {@link String}/{@link Integer}), built from a
 * {@link ParsedProtoSchema} - the Protobuf equivalent of
 * {@link io.kroxylicious.filter.record.manipulation.avro.AvroFunction}.
 * <p>
 * Unlike Avro's array/{@code items} schema, a Protobuf {@code repeated} field has no separate node to hang
 * a per-element {@code apply} chain off - repeated-ness and element type live on the one
 * {@link Descriptors.FieldDescriptor}. So an {@code apply} declared on a repeated field is deliberately
 * interpreted as per-element (masking each element the way Avro's array {@code items.apply} does), rather
 * than as a whole-list operation (which nothing in {@code common} implements anyway) - a Protobuf-specific
 * design choice forced by its schema shape, not an accident of mirroring Avro too literally.
 */
public interface ProtoFunction extends BiFunction<Object, Context, Object> {

    /**
     * Builds a mask function from a {@link ParsedProtoSchema}, with no additional requirement beyond each
     * field's {@code apply} chain composing.
     * @param schema the parsed schema, plus its {@code apply} chains
     * @return a function transforming an input value according to {@code schema}, given a {@link Context}
     */
    static ProtoFunction buildMask(ParsedProtoSchema schema) {
        return buildMask(schema, Set.of());
    }

    /**
     * Builds a mask function from a {@link ParsedProtoSchema}.
     * @param schema the parsed schema, plus its {@code apply} chains
     * @param requirements properties every field's composed {@code apply} chain must satisfy
     * @return a function transforming an input value according to {@code schema}, given a {@link Context}
     */
    static ProtoFunction buildMask(ParsedProtoSchema schema, Set<Requirement> requirements) {
        return buildMask(schema.descriptor(), schema.apply(), requirements);
    }

    private static ProtoFunction buildMask(Descriptors.Descriptor descriptor, Map<Descriptors.GenericDescriptor, List<ApplyConfig>> applyByNode,
                                           Set<Requirement> requirements) {
        ProtoFunction structural = buildStructural(descriptor, applyByNode, requirements);
        if (applyByNode.get(descriptor) != null) {
            throw new IllegalArgumentException("apply is not yet supported for message-level nodes: " + descriptor.getFullName());
        }
        return structural;
    }

    /**
     * Binds a fixed {@link Context} to this function, producing a plain {@code Function<Object,Object>}.
     * Kept as loosely typed as the function itself since a mask can be built from a non-message schema too
     * (e.g. one describing just a repeated field or a leaf) - see {@link #bindRecord(Context)} for the
     * common case of composing a whole-message mask into a
     * {@link io.kroxylicious.filter.record.manipulation.common.Pipeline} between a deserializer and
     * serializer, which are both typed to {@link DynamicMessage}.
     * @param context the context to bind
     * @return an equivalent {@code Function<Object,Object>}
     */
    default Function<Object, Object> bind(Context context) {
        return new BoundProtoFunction(this, context);
    }

    /**
     * Binds a fixed {@link Context} to this function, producing a {@code Function<DynamicMessage,DynamicMessage>}
     * suitable for composing into a whole-message {@link io.kroxylicious.filter.record.manipulation.common.Pipeline}
     * stage alongside {@link ProtoBinaryDeserializer}/{@link ProtoBinarySerializer}, which are themselves
     * typed to {@link DynamicMessage} rather than {@code Object} - narrower than {@link #bind(Context)}
     * purely so {@link io.kroxylicious.filter.record.manipulation.common.Pipeline}'s reflection-based
     * composition check lines up either side of this stage, not because the underlying transformation
     * differs.
     * @param context the context to bind
     * @return an equivalent {@code Function<DynamicMessage,DynamicMessage>}
     * @throws ClassCastException if this function was built from a non-message schema
     */
    default Function<DynamicMessage, DynamicMessage> bindRecord(Context context) {
        return new BoundProtoMessageFunction(this, context);
    }

    /**
     * A concrete (non-lambda) {@link Function} binding a fixed {@link Context} to a {@link ProtoFunction} -
     * see {@code AvroFunction.BoundAvroFunction} for why this needs to be a named class.
     * @param fn the function being bound
     * @param context the context bound to it
     */
    record BoundProtoFunction(ProtoFunction fn, Context context) implements Function<Object, Object> {
        @Override
        public Object apply(Object value) {
            return fn.apply(value, context);
        }
    }

    /**
     * The {@link DynamicMessage}-typed counterpart of {@link BoundProtoFunction}, for the same reason - see
     * {@link #bindRecord(Context)}.
     * @param fn the function being bound
     * @param context the context bound to it
     */
    record BoundProtoMessageFunction(ProtoFunction fn, Context context) implements Function<DynamicMessage, DynamicMessage> {
        @Override
        public DynamicMessage apply(DynamicMessage value) {
            return (DynamicMessage) fn.apply(value, context);
        }
    }

    /**
     * Builds the part of the mask that recurses into a message's declared fields, leaving leaves
     * untouched. This runs before each field's own {@code apply} chain (if any), so {@code apply} always
     * sees the already-masked value - mirrors {@code AvroFunction.buildStructural}.
     */
    private static ProtoFunction buildStructural(Descriptors.Descriptor descriptor, Map<Descriptors.GenericDescriptor, List<ApplyConfig>> applyByNode,
                                                 Set<Requirement> requirements) {
        Map<String, BiFunction<Object, Context, Object>> mapping = descriptor.getFields().stream()
                .collect(Collectors.toMap(Descriptors.FieldDescriptor::getName, field -> fieldFunction(field, applyByNode, requirements), (a, b) -> a,
                        LinkedHashMap::new));
        var fn = ProtoMessages.mapFields(descriptor, mapping);
        return (value, context) -> fn.apply((DynamicMessage) value, context);
    }

    /**
     * Builds a field's mapping function: the field's own type-driven mask (recursing into a nested message
     * type), composed with any {@code apply} declared on the field itself, then - if the field is
     * {@code repeated} - lifted to run per-element (see the class javadoc for why {@code apply} is
     * per-element rather than whole-list here).
     */
    private static BiFunction<Object, Context, Object> fieldFunction(Descriptors.FieldDescriptor field,
                                                                     Map<Descriptors.GenericDescriptor, List<ApplyConfig>> applyByNode,
                                                                     Set<Requirement> requirements) {
        if (field.isMapField()) {
            throw new IllegalArgumentException("Proto mask not yet supported for map fields: " + field.getFullName());
        }
        ProtoFunction elementFn = buildElementTypeMask(field, applyByNode, requirements);
        List<ApplyConfig> apply = applyByNode.get(field);
        if (apply != null) {
            ProtoFunction ownApply = buildApplyChain(field, apply, requirements);
            ProtoFunction base = elementFn;
            elementFn = (value, context) -> ownApply.apply(base.apply(value, context), context);
        }
        if (field.isRepeated()) {
            ProtoFunction perElement = elementFn;
            return (value, context) -> new ListElements().modifyAll(castToList(value), perElement, context);
        }
        return elementFn;
    }

    /**
     * Builds the mask for one element of {@code field} (its message type, if any - recursing via
     * {@link #buildMask(Descriptors.Descriptor, Map, Set)} - or a passthrough for a supported scalar leaf
     * type), ignoring {@code field}'s repeated-ness, which {@link #fieldFunction} applies separately.
     */
    private static ProtoFunction buildElementTypeMask(Descriptors.FieldDescriptor field, Map<Descriptors.GenericDescriptor, List<ApplyConfig>> applyByNode,
                                                      Set<Requirement> requirements) {
        return switch (field.getType()) {
            case MESSAGE -> buildMask(field.getMessageType(), applyByNode, requirements);
            case STRING, INT32 -> (value, context) -> value;
            default -> throw new IllegalArgumentException("Proto mask not yet supported for field type: " + field.getType());
        };
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castToList(Object value) {
        return (List<Object>) value;
    }

    /**
     * Builds and composes a field's own {@code apply} list into a single function, via
     * {@link ContextPipeline} - mirrors {@code AvroFunction.buildApplyChain}, minus {@code delete}:
     * removing a field isn't meaningful without deciding what it means for a required proto2/proto3
     * implicit-presence field, so it fails loudly rather than producing a possibly-nonconforming message.
     */
    private static ProtoFunction buildApplyChain(Descriptors.FieldDescriptor field, List<ApplyConfig> ops, Set<Requirement> requirements) {
        return switch (field.getType()) {
            case STRING -> {
                List<BiFunction<?, Context, ?>> fns = ops.stream().<BiFunction<?, Context, ?>> map(ProtoFunction::buildStringOp).toList();
                ContextPipeline pipeline = new ContextPipeline(fns, requirements);
                yield (value, context) -> pipeline.<String, String> apply(value == null ? null : value.toString(), context);
            }
            case INT32 -> {
                List<BiFunction<?, Context, ?>> fns = ops.stream().<BiFunction<?, Context, ?>> map(ProtoFunction::buildIntegerOp).toList();
                ContextPipeline pipeline = new ContextPipeline(fns, requirements);
                yield (value, context) -> pipeline.<Integer, Integer> apply((Integer) value, context);
            }
            default -> throw new IllegalArgumentException("apply is not yet supported for field type: " + field.getType());
        };
    }

    private static StringOp buildStringOp(ApplyConfig op) {
        if (Boolean.TRUE.equals(op.delete())) {
            throw new IllegalArgumentException("delete is not yet supported for Protobuf fields");
        }
        else if (op.value() != null) {
            String constant = op.value().textValue();
            return (ignored, context) -> constant;
        }
        else if (op.random() != null) {
            var generator = new RandomStringSupplier(op.random().alphabet(), op.random().minLength(), op.random().maxLength());
            return (ignored, context) -> generator.apply(context);
        }
        else if (op.choose() != null) {
            Set<String> from = op.choose().stream().map(x -> (String) x).collect(Collectors.toSet());
            var generator = new ChooseStringSupplier(from);
            return (ignored, context) -> generator.apply(context);
        }
        else if (op.hmac() != null) {
            var fn = new HmacStringFunction();
            return (value, context) -> value == null ? null : fn.apply(value, context);
        }
        else if (op.encrypt() != null) {
            var fn = new EncryptStringFunction();
            return (value, context) -> value == null ? null : fn.apply(value, context);
        }
        else if (op.decrypt() != null) {
            var fn = new DecryptStringFunction();
            return (value, context) -> value == null ? null : fn.apply(value, context);
        }
        else {
            return (value, context) -> value;
        }
    }

    /**
     * Resolves and builds a pluggable operation, e.g. {@code op: RandomInt} - proof of concept for making
     * the {@code apply} vocabulary extensible via Kroxylicious's Plugin mechanism. Not yet wired into
     * {@link #buildApplyChain}: {@link ApplyConfig}'s closed operation vocabulary is unaffected by this.
     */
    static IntOp buildIntegerOp(OpConfig op, PluginLookup lookup) {
        IntOpFactory factory = lookup.pluginInstance(IntOpFactory.class, op.op());
        return factory.create(op.config());
    }

    private static IntOp buildIntegerOp(ApplyConfig op) {
        if (Boolean.TRUE.equals(op.delete())) {
            throw new IllegalArgumentException("delete is not yet supported for Protobuf fields");
        }
        else if (op.value() != null) {
            int constant = op.value().intValue();
            return (ignored, context) -> constant;
        }
        else if (op.random() != null) {
            var generator = new RandomIntSupplier(op.random().min(), op.random().max());
            return (ignored, context) -> generator.applyAsInt(context);
        }
        else if (op.choose() != null) {
            Set<Integer> from = op.choose().stream().map(x -> (Integer) x).collect(Collectors.toSet());
            var generator = new ChooseIntSupplier(from);
            return (ignored, context) -> generator.applyAsInt(context);
        }
        else {
            return (value, context) -> value;
        }
    }
}
