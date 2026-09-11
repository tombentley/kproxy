/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson2;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.kroxylicious.filter.record.manipulation.common.Maybe;
import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.Requirement;
import io.kroxylicious.filter.record.manipulation.common.StaticTypedOp;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;
import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpConfig;
import io.kroxylicious.filter.record.manipulation.op.OpContext;

/**
 * A mask/transform over a {@link JsonNode}, built from a {@link SchemaConfig} tree - or, invoked with
 * {@link MissingNode#getInstance()} as the input, a generator: root/whole-record generation is simply this
 * same traversal started from nothing instead of a real value, rather than a separate code path.
 *
 */
public class JacksonFunction implements BaseTypedOp<JsonNode, JsonNode> {

    private final BiFunction<JsonNode, OpContext, JsonNode> fn;

    public JacksonFunction(BiFunction<JsonNode, OpContext, JsonNode> fn) {
        this.fn = fn;
    }

    @Override
    public Type inputType() {
        return JsonNode.class;
    }

    @Override
    public Type outputType() {
        return JsonNode.class;
    }

    @Override
    public JsonNode apply(JsonNode value, OpContext opContext) {
        return fn.apply(value, opContext);
    }

    /**
     * Builds a mask/generator function from a {@link SchemaConfig} tree, with no additional requirement
     * beyond each field's {@code apply} chain composing.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @param lookup resolves the plugin implementation named by each {@code apply} entry's {@code op}
     * @return a function transforming an input {@link JsonNode} according to {@code schema}, given a
     *         {@link OpContext}
     */
    static JacksonFunction buildMask(SchemaConfig schema,
                                     PluginLookup lookup) {
        return buildMask(schema, Set.of(), lookup);
    }

    /**
     * Builds a mask/generator function from a {@link SchemaConfig} tree.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @param requirements properties every field's composed {@code apply} chain must satisfy
     * @param lookup resolves the plugin implementation named by each {@code apply} entry's {@code op}
     * @return a function transforming an input {@link JsonNode} according to {@code schema}, given a
     *         {@link OpContext}
     */
    static JacksonFunction buildMask(SchemaConfig schema,
                                     Set<Requirement> requirements,
                                     PluginLookup lookup) {
        JacksonFunction structural = buildStructural(schema, requirements, lookup);
        JacksonFunction foo;
        if (schema.apply() != null) {
            JacksonFunction ownApply = buildApplyChain(schema.type(), schema.apply(), requirements, lookup);
            foo = new JacksonFunction((node, opContext) -> ownApply.apply(structural.apply(node, opContext), opContext));
        }
        else {
            foo = structural;
        }
        return foo;
    }
    //
    // /**
    // * Binds a fixed {@link OpContext} to this function, producing a plain {@code Function<JsonNode,JsonNode>}
    // * suitable for composing into a whole-record {@link Pipeline}
    // * stage, which (unlike {@link OpPipeline}) has no notion of {@link OpContext} - see the module README
    // * for why {@code Pipeline} stays that way.
    // * @param opContext the context to bind
    // * @return an equivalent {@code Function<JsonNode,JsonNode>}
    // */
    // default Function<JsonNode, JsonNode> bind(OpContext opContext) {
    // return new BoundJacksonFunction(this, opContext);
    // }

    /**
     * Adapts this function to the {@link Maybe}-based convention {@link ObjectNodes#mapProperties} uses, from
     * this interface's own {@link MissingNode}-sentinel convention.
     * @return an equivalent {@code BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>>}
     */
    BaseTypedOp<Maybe<JsonNode>, Maybe<JsonNode>> asMaybe() {
        return new StaticTypedOp<Maybe<JsonNode>, Maybe<JsonNode>>() {

            @Override
            public Maybe<JsonNode> apply(Maybe<JsonNode> maybe, OpContext opContext) {
                JsonNode input = maybe instanceof Maybe.Some<JsonNode> some ? some.value() : MissingNode.getInstance();
                JsonNode output = JacksonFunction.this.apply(input, opContext);
                return output.isMissingNode() ? Maybe.none() : Maybe.some(output);
            }
        };
    }

    // /**
    // * A concrete (non-lambda) {@link Function} binding a fixed {@link OpContext} to a {@link JacksonFunction}.
    // * Concrete classes are reflectable via their own declaration regardless of whether they implement a
    // * fixed-type marker interface; only lambdas need one (a bare {@code node -> fn.apply(node, context)}
    // * lambda would not be independently reflectable).
    // * @param fn the function being bound
    // * @param opContext the context bound to it
    // */
    // record BoundJacksonFunction(JacksonFunction fn, OpContext opContext) implements Function<JsonNode, JsonNode> {
    // @Override
    // public JsonNode apply(JsonNode node) {
    // return fn.apply(node, opContext);
    // }
    // }

    /**
     * Builds the part of the mask that recurses into a node's declared children ({@code properties}/{@code items}),
     * leaving leaves untouched. This runs before the node's own {@code apply} chain (if any), so {@code apply}
     * always sees the already-masked children.
     */
    private static JacksonFunction buildStructural(SchemaConfig schema,
                                                   Set<Requirement> requirements,
                                                   PluginLookup lookup) {
        return switch (schema.type()) {
            case "array" -> {
                if (schema.items() != null) {
                    var fn = ArrayNodes.items(buildMask(schema.items(), requirements, lookup));
                    // No speculative materialization for arrays: items() maps whatever elements already
                    // exist, and there's no concept of synthesizing new elements from nothing yet.
                    yield new JacksonFunction((node, context) -> node.isMissingNode() ? node : fn.apply((ArrayNode) node, context));
                }
                else {
                    yield new JacksonFunction((node, context) -> node);
                }
            }
            case "object" -> {
                if (schema.properties() != null) {
                    Map<String, BaseTypedOp<Maybe<JsonNode>, Maybe<JsonNode>>> mapping = schema.properties().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                    entry -> buildMask(entry.getValue(), requirements, lookup).asMaybe(),
                                    (a, b) -> a, LinkedHashMap::new));
                    var fn = new ObjectNodes(JsonNodeFactory.instance).mapProperties(mapping);
                    // Speculatively recurse into a fresh empty object even when this node itself is
                    // missing, so a generator-shaped apply chain on a declared child (at any depth) still
                    // gets a chance to insert. Collapse back to missing if nothing real came of it, but
                    // only when this node was already missing - a genuinely-present object that ends up
                    // empty (e.g. every property deleted) must never be silently discarded.
                    yield new JacksonFunction((node, context) -> {
                        ObjectNode input = node.isMissingNode() ? JsonNodeFactory.instance.objectNode() : (ObjectNode) node;
                        ObjectNode result = fn.apply(input, context);
                        return result.isEmpty() && node.isMissingNode() ? MissingNode.getInstance() : result;
                    });
                }
                else {
                    yield new JacksonFunction((node, context) -> node);
                }
            }
            case "boolean", "integer", "number", "string" -> new JacksonFunction((node, context) -> node);
            default -> throw new IllegalArgumentException("Invalid mask type: " + schema.type());
        };
    }

    private static JacksonFunction buildApplyChain(String type,
                                                   List<OpConfig> opConfigs,
                                                   Set<Requirement> requirements,
                                                   PluginLookup lookup) {
        return switch (type) {
            case "boolean" -> {
                var pipeline = OpConfigs.compose(Boolean.class, opConfigs, requirements, lookup);
                yield new JacksonFunction((node, context) -> {
                    Boolean result = (Boolean) pipeline.apply(node.isMissingNode() ? null : node.asBoolean(), context);
                    return result == null ? MissingNode.getInstance() : result ? BooleanNode.getTrue() : BooleanNode.getFalse();
                });
            }
            case "integer" -> {
                // TODO need to handle short, long and BigInteger
                var pipeline = OpConfigs.compose(Integer.class, opConfigs, requirements, lookup);
                yield new JacksonFunction((node, context) -> {
                    Integer result = (Integer) pipeline.apply(node.isMissingNode() ? null : node.asInt(), context);
                    return result == null ? MissingNode.getInstance() : new IntNode(result);
                });
            }
            case "number" -> {
                // TODO need to handle float and BigDecimal
                var pipeline = OpConfigs.compose(Double.class, opConfigs, requirements, lookup);
                yield new JacksonFunction((node, context) -> {
                    Double result = (Double) pipeline.apply(node.isMissingNode() ? null : node.asDouble(), context);
                    return result == null ? MissingNode.getInstance() : new DoubleNode(result);
                });
            }
            case "string" -> {
                var pipeline = OpConfigs.compose(String.class, opConfigs, requirements, lookup);
                yield new JacksonFunction((node, context) -> {
                    String result = (String) pipeline.apply(node.isMissingNode() ? null : node.asText(), context);
                    return result == null ? MissingNode.getInstance() : new TextNode(result);
                });
            }

            default -> throw new IllegalArgumentException("apply is not yet supported for type " + type);
        };
    }

    //
    // @NonNull
    // private static <T, R> OpPipeline<T, R> contextPipeline(List<OpConfig> ops,
    // Set<Requirement> requirements,
    // PluginLookup lookup,
    // BiFunction<OpConfig, PluginLookup, TypedOp<T, R>> factoryFn) {
    //
    // List<TypedOp<?, ?>> fns = ops.stream().<TypedOp<?, ?>> map(op -> factoryFn.apply(op, lookup)).toList();
    // return new OpPipeline<>(fns, requirements);
    // }

    // /**
    // * Resolves one {@code apply} entry to a {@link TypedOp} - {@link OpConfigs#DELETE} is special-cased
    // * here (rather than resolved via {@code lookup}) since Jackson can represent "this property is absent"
    // * ({@link MissingNode}), unlike Avro/Protobuf (see {@code AvroFunction}/{@code ProtoFunction}'s
    // * equivalents, which reject it instead).
    // */
    // private static <T, R> TypedOp<T, R> buildOp(OpConfig op, Class<T> inputType, PluginLookup lookup) {
    // if (OpConfigs.DELETE.equals(op.op())) {
    // return new TypedOp<T, R>() {
    // @Override
    // public Type outputType(Type inputType) {
    // return null;
    // }
    //
    // @Override
    // public R apply(T value, OpContext opContext) {
    // return null;
    // }
    // };
    // }
    // return OpConfigs.resolveOp(op, inputType, outputType, lookup);
    // }
}
