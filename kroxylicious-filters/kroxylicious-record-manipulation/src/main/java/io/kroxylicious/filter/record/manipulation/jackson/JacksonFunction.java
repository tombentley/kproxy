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

import io.kroxylicious.filter.record.manipulation.common.ChooseIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.ChooseStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.ContextPipeline;
import io.kroxylicious.filter.record.manipulation.common.DecryptStringFunction;
import io.kroxylicious.filter.record.manipulation.common.EncryptStringFunction;
import io.kroxylicious.filter.record.manipulation.common.HmacStringFunction;
import io.kroxylicious.filter.record.manipulation.common.IntOp;
import io.kroxylicious.filter.record.manipulation.common.RandomIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.RandomStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.Requirement;
import io.kroxylicious.filter.record.manipulation.common.StringOp;
import io.kroxylicious.filter.record.manipulation.config.ApplyConfig;
import io.kroxylicious.filter.record.manipulation.config.SchemaConfig;

/**
 * A mask/transform over a {@link JsonNode}, built from a {@link SchemaConfig} tree - or, invoked with
 * {@link MissingNode#getInstance()} as the input, a generator: root/whole-record generation is simply this
 * same traversal started from nothing instead of a real value, rather than a separate code path.
 * <p>
 * This is declared as its own interface (rather than using {@code BiFunction<JsonNode, Context, JsonNode>}
 * directly) so that instances built by {@link #buildMask(SchemaConfig)} carry a fixed, reflectable generic
 * signature - see {@link ContextPipeline} for why that matters.
 */
public interface JacksonFunction extends BiFunction<JsonNode, Context, JsonNode> {

    /**
     * Builds a mask/generator function from a {@link SchemaConfig} tree, with no additional requirement
     * beyond each field's {@code apply} chain composing.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @return a function transforming an input {@link JsonNode} according to {@code schema}, given a
     *         {@link Context}
     */
    static JacksonFunction buildMask(SchemaConfig schema) {
        return buildMask(schema, Set.of());
    }

    /**
     * Builds a mask/generator function from a {@link SchemaConfig} tree.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @param requirements properties every field's composed {@code apply} chain must satisfy
     * @return a function transforming an input {@link JsonNode} according to {@code schema}, given a
     *         {@link Context}
     */
    static JacksonFunction buildMask(SchemaConfig schema, Set<Requirement> requirements) {
        JacksonFunction structural = buildStructural(schema, requirements);
        if (schema.apply() == null) {
            return structural;
        }
        JacksonFunction ownApply = buildApplyChain(schema.type(), schema.apply(), requirements);
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
    private static JacksonFunction buildStructural(SchemaConfig schema, Set<Requirement> requirements) {
        return switch (schema.type()) {
            case "array" -> {
                if (schema.items() != null) {
                    var fn = ArrayNodes.items(buildMask(schema.items(), requirements));
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
                    Map<String, JacksonFunction> mapping = schema.properties().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> buildMask(e.getValue(), requirements), (a, b) -> a, LinkedHashMap::new));
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
    private static JacksonFunction buildApplyChain(String type, List<ApplyConfig> ops, Set<Requirement> requirements) {
        return switch (type) {
            case "string" -> {
                List<BiFunction<?, Context, ?>> fns = ops.stream().<BiFunction<?, Context, ?>> map(JacksonFunction::buildStringOp).toList();
                ContextPipeline pipeline = new ContextPipeline(fns, requirements);
                yield (node, context) -> {
                    String result = pipeline.<String, String> apply(node.isMissingNode() ? null : node.asText(), context);
                    return result == null ? MissingNode.getInstance() : new TextNode(result);
                };
            }
            case "integer" -> {
                List<BiFunction<?, Context, ?>> fns = ops.stream().<BiFunction<?, Context, ?>> map(JacksonFunction::buildIntegerOp).toList();
                ContextPipeline pipeline = new ContextPipeline(fns, requirements);
                yield (node, context) -> {
                    Integer result = pipeline.<Integer, Integer> apply(node.isMissingNode() ? null : node.asInt(), context);
                    return result == null ? MissingNode.getInstance() : new IntNode(result);
                };
            }
            default -> throw new IllegalArgumentException("apply is not yet supported for type " + type);
        };
    }

    private static StringOp buildStringOp(ApplyConfig op) {
        if (Boolean.TRUE.equals(op.delete())) {
            return (value, context) -> null;
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
            // hmac/encrypt/decrypt are transformers requiring a real prior value, unlike the generators
            // above (which already ignore their input unconditionally): a null input means "there is
            // nothing here to transform", so pass it straight through rather than crashing on it.
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

    private static IntOp buildIntegerOp(ApplyConfig op) {
        if (Boolean.TRUE.equals(op.delete())) {
            return (value, context) -> null;
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
