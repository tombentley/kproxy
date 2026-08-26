/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.ContextPipeline;
import io.kroxylicious.filter.record.manipulation.common.IntOp;
import io.kroxylicious.filter.record.manipulation.common.Maybe;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.Requirement;
import io.kroxylicious.filter.record.manipulation.common.StringOp;
import io.kroxylicious.filter.record.manipulation.config.OpConfig;
import io.kroxylicious.filter.record.manipulation.config.OpConfigs;

/**
 * A mask/transform over a {@link JsonNode}, built from a {@link SchemaConfig} tree - or, invoked with
 * {@link MissingNode#getInstance()} as the input, a generator: root/whole-record generation is simply this
 * same traversal started from nothing instead of a real value, rather than a separate code path.
 * <p>
 * This is declared as its own interface (rather than using {@code BiFunction<JsonNode, Context, JsonNode>}
 * directly) so that instances built by {@link #buildMask(SchemaConfig, PluginLookup)} carry a fixed,
 * reflectable generic signature - see {@link ContextPipeline} for why that matters.
 */
public interface JacksonFunction extends BiFunction<JsonNode, Context, JsonNode> {

    /**
     * Builds a mask/generator function from a {@link SchemaConfig} tree, with no additional requirement
     * beyond each field's {@code apply} chain composing.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @param lookup resolves the plugin implementation named by each {@code apply} entry's {@code op}
     * @return a function transforming an input {@link JsonNode} according to {@code schema}, given a
     *         {@link Context}
     */
    static JacksonFunction buildMask(SchemaConfig schema, PluginLookup lookup) {
        return buildMask(schema, Set.of(), lookup);
    }

    /**
     * Builds a mask/generator function from a {@link SchemaConfig} tree.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @param requirements properties every field's composed {@code apply} chain must satisfy
     * @param lookup resolves the plugin implementation named by each {@code apply} entry's {@code op}
     * @return a function transforming an input {@link JsonNode} according to {@code schema}, given a
     *         {@link Context}
     */
    static JacksonFunction buildMask(SchemaConfig schema, Set<Requirement> requirements, PluginLookup lookup) {
        JacksonFunction structural = buildStructural(schema, requirements, lookup);
        if (schema.apply() == null) {
            return structural;
        }
        JacksonFunction ownApply = buildApplyChain(schema.type(), schema.apply(), requirements, lookup);
        return (node, context) -> ownApply.apply(structural.apply(node, context), context);
    }

    /**
     * Binds a fixed {@link Context} to this function, producing a plain {@code Function<JsonNode,JsonNode>}
     * suitable for composing into a whole-record {@link io.kroxylicious.filter.record.manipulation.common.Pipeline}
     * stage, which (unlike {@link ContextPipeline}) has no notion of {@link Context} - see the module README
     * for why {@code Pipeline} stays that way.
     * @param context the context to bind
     * @return an equivalent {@code Function<JsonNode,JsonNode>}
     */
    default Function<JsonNode, JsonNode> bind(Context context) {
        return new BoundJacksonFunction(this, context);
    }

    /**
     * Adapts this function to the {@link Maybe}-based convention {@link ObjectNodes#mapProperties} uses, from
     * this interface's own {@link MissingNode}-sentinel convention.
     * @return an equivalent {@code BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>>}
     */
    default BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>> asMaybe() {
        return (maybe, context) -> {
            JsonNode input = maybe instanceof Maybe.Some<JsonNode> some ? some.value() : MissingNode.getInstance();
            JsonNode output = apply(input, context);
            return output.isMissingNode() ? Maybe.none() : Maybe.some(output);
        };
    }

    /**
     * A concrete (non-lambda) {@link Function} binding a fixed {@link Context} to a {@link JacksonFunction}.
     * Concrete classes are reflectable via their own declaration regardless of whether they implement a
     * fixed-type marker interface; only lambdas need one (a bare {@code node -> fn.apply(node, context)}
     * lambda would not be independently reflectable).
     * @param fn the function being bound
     * @param context the context bound to it
     */
    record BoundJacksonFunction(JacksonFunction fn, Context context) implements Function<JsonNode, JsonNode> {
        @Override
        public JsonNode apply(JsonNode node) {
            return fn.apply(node, context);
        }
    }

    /**
     * Builds the part of the mask that recurses into a node's declared children ({@code properties}/{@code items}),
     * leaving leaves untouched. This runs before the node's own {@code apply} chain (if any), so {@code apply}
     * always sees the already-masked children.
     */
    private static JacksonFunction buildStructural(SchemaConfig schema, Set<Requirement> requirements, PluginLookup lookup) {
        return switch (schema.type()) {
            case "array" -> {
                if (schema.items() != null) {
                    var fn = ArrayNodes.items(buildMask(schema.items(), requirements, lookup));
                    // No speculative materialization for arrays: items() maps whatever elements already
                    // exist, and there's no concept of synthesizing new elements from nothing yet.
                    yield (node, context) -> node.isMissingNode() ? node : fn.apply((ArrayNode) node, context);
                }
                else {
                    yield (node, context) -> node;
                }
            }
            case "object" -> {
                if (schema.properties() != null) {
                    Map<String, BiFunction<Maybe<JsonNode>, Context, Maybe<JsonNode>>> mapping = schema.properties().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> buildMask(e.getValue(), requirements, lookup).asMaybe(), (a, b) -> a, LinkedHashMap::new));
                    var fn = new ObjectNodes(JsonNodeFactory.instance).mapProperties(mapping);
                    // Speculatively recurse into a fresh empty object even when this node itself is
                    // missing, so a generator-shaped apply chain on a declared child (at any depth) still
                    // gets a chance to insert. Collapse back to missing if nothing real came of it, but
                    // only when this node was already missing - a genuinely-present object that ends up
                    // empty (e.g. every property deleted) must never be silently discarded.
                    yield (node, context) -> {
                        ObjectNode input = node.isMissingNode() ? JsonNodeFactory.instance.objectNode() : (ObjectNode) node;
                        ObjectNode result = fn.apply(input, context);
                        return result.isEmpty() && node.isMissingNode() ? MissingNode.getInstance() : result;
                    };
                }
                else {
                    yield (node, context) -> node;
                }
            }
            case "string", "integer" -> (node, context) -> node;
            default -> throw new IllegalArgumentException("Invalid mask type: " + schema.type());
        };
    }

    /**
     * Builds and composes a node's own {@code apply} list into a single function, via {@link ContextPipeline}.
     * Composes at the tightly-typed {@code common} level (not the loosely-typed {@link JsonNode} level) so
     * {@link ContextPipeline}'s composition check is meaningful, wrapping into/out of {@link JsonNode} only
     * at the two ends. Within that tightly-typed domain, a Java {@code null} is the "no value" sentinel (an
     * op-level {@code delete} produces it; a transformer passes an incoming {@code null} straight through) -
     * translated to/from {@link MissingNode} only at this method's boundary, never leaking further.
     */
    private static JacksonFunction buildApplyChain(String type, List<OpConfig> ops, Set<Requirement> requirements, PluginLookup lookup) {
        return switch (type) {
            case "string" -> {
                List<BiFunction<?, Context, ?>> fns = ops.stream().<BiFunction<?, Context, ?>> map(op -> buildStringOp(op, lookup)).toList();
                ContextPipeline pipeline = new ContextPipeline(fns, requirements);
                yield (node, context) -> {
                    String result = pipeline.<String, String> apply(node.isMissingNode() ? null : node.asText(), context);
                    return result == null ? MissingNode.getInstance() : new TextNode(result);
                };
            }
            case "integer" -> {
                List<BiFunction<?, Context, ?>> fns = ops.stream().<BiFunction<?, Context, ?>> map(op -> buildIntegerOp(op, lookup)).toList();
                ContextPipeline pipeline = new ContextPipeline(fns, requirements);
                yield (node, context) -> {
                    Integer result = pipeline.<Integer, Integer> apply(node.isMissingNode() ? null : node.asInt(), context);
                    return result == null ? MissingNode.getInstance() : new IntNode(result);
                };
            }
            default -> throw new IllegalArgumentException("apply is not yet supported for type " + type);
        };
    }

    /**
     * Resolves one {@code apply} entry to a {@link StringOp} - {@link OpConfigs#DELETE} is special-cased
     * here (rather than resolved via {@code lookup}) since Jackson can represent "this property is absent"
     * ({@link MissingNode}), unlike Avro/Protobuf (see {@code AvroFunction}/{@code ProtoFunction}'s
     * equivalents, which reject it instead).
     */
    private static StringOp buildStringOp(OpConfig op, PluginLookup lookup) {
        if (OpConfigs.DELETE.equals(op.op())) {
            return (value, context) -> null;
        }
        return OpConfigs.resolveStringOp(op, lookup);
    }

    /**
     * The {@link IntOp} counterpart of {@link #buildStringOp(OpConfig, PluginLookup)}.
     */
    private static IntOp buildIntegerOp(OpConfig op, PluginLookup lookup) {
        if (OpConfigs.DELETE.equals(op.op())) {
            return (value, context) -> null;
        }
        return OpConfigs.resolveIntOp(op, lookup);
    }
}
