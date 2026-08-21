/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.kroxylicious.filter.record.manipulation.common.ChooseIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.ChooseStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.ConstantIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.ConstantStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.DecryptStringFunction;
import io.kroxylicious.filter.record.manipulation.common.EncryptStringFunction;
import io.kroxylicious.filter.record.manipulation.common.HmacStringFunction;
import io.kroxylicious.filter.record.manipulation.common.Pipeline;
import io.kroxylicious.filter.record.manipulation.common.RandomIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.RandomStringSupplier;
import io.kroxylicious.filter.record.manipulation.config.ApplyConfig;
import io.kroxylicious.filter.record.manipulation.config.SchemaConfig;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A mask/transform over a {@link JsonNode}, built from a {@link SchemaConfig} tree.
 * <p>
 * This is declared as its own interface (rather than using {@code Function<JsonNode, JsonNode>} directly)
 * so that instances built by {@link #buildMask(SchemaConfig)} carry a fixed, reflectable generic signature.
 * {@link io.kroxylicious.filter.record.manipulation.common.Pipeline} relies on this to check that a
 * {@code JacksonFunction} composes with the stage before it (e.g. a {@link JacksonDeserializer}).
 */
public interface JacksonFunction extends Function<JsonNode, JsonNode> {

    /**
     * Builds a mask function from a {@link SchemaConfig} tree.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @return a function transforming an input {@link JsonNode} according to {@code schema}
     */
    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "The PRNG is deliberately not SecureRandom: we want control over "
            + "the seed (e.g. derived from topic/partition/offset) so masking can eventually have repeatable-read semantics on the Fetch path")
    static JacksonFunction buildMask(SchemaConfig schema) {
        return buildMask(schema, new Random());
    }

    /**
     * Builds a mask function from a {@link SchemaConfig} tree, drawing all randomness (random/choose values,
     * encryption IVs) from the given {@code random} instance, threaded through every recursive call.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @param random the source of randomness for this schema tree and all its nested masks
     * @return a function transforming an input {@link JsonNode} according to {@code schema}
     */
    static JacksonFunction buildMask(SchemaConfig schema, Random random) {
        byte[] key = new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };

        JacksonFunction structural = buildStructural(schema, random);
        if (schema.apply() == null) {
            return structural;
        }

        JacksonFunction ownApply = buildApplyChain(schema.type(), schema.apply(), key, random);
        return node -> ownApply.apply(structural.apply(node));
    }

    /**
     * Builds the part of the mask that recurses into a node's declared children ({@code properties}/{@code items}),
     * leaving leaves untouched. This runs before the node's own {@code apply} chain (if any), so {@code apply}
     * always sees the already-masked children.
     */
    private static JacksonFunction buildStructural(SchemaConfig schema, Random random) {
        return switch (schema.type()) {
            case "array" -> {
                if (schema.items() != null) {
                    var fn = new ArrayNodes(JsonNodeFactory.instance).items(buildMask(schema.items(), random));
                    yield node -> fn.apply((ArrayNode) node);
                }
                else {
                    yield node -> node;
                }
            }
            case "object" -> {
                if (schema.properties() != null) {
                    Map<String, JacksonFunction> mapping = schema.properties().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> buildMask(e.getValue(), random)));
                    var fn = new ObjectNodes(JsonNodeFactory.instance).mapProperties(mapping);
                    yield node -> fn.apply((ObjectNode) node);
                }
                else {
                    yield node -> node;
                }
            }
            case "string", "integer" -> node -> node;
            default -> throw new IllegalArgumentException("Invalid mask type: " + schema.type());
        };
    }

    /**
     * Builds and composes a node's own {@code apply} list into a single function, via {@link Pipeline}.
     */
    private static JacksonFunction buildApplyChain(String type, List<ApplyConfig> ops, byte[] key, Random random) {
        List<Function<?, ?>> fns = ops.stream().<Function<?, ?>> map(op -> buildOp(type, op, key, random)).toList();
        Pipeline fieldPipeline = new Pipeline(fns);
        return node -> fieldPipeline.<JsonNode, JsonNode> apply(node);
    }

    private static JacksonFunction buildOp(String type, ApplyConfig op, byte[] key, Random random) {
        return switch (type) {
            case "string" -> buildStringOp(op, key, random);
            case "integer" -> buildIntegerOp(op, random);
            default -> throw new IllegalArgumentException("apply is not yet supported for type " + type);
        };
    }

    private static JacksonFunction buildStringOp(ApplyConfig op, byte[] key, Random random) {
        if (op.value() != null) {
            var supplier = Jackson.convertString(new ConstantStringSupplier(op.value().textValue()));
            return ignored -> supplier.get();
        }
        else if (op.random() != null) {
            var supplier = Jackson.convertString(
                    new RandomStringSupplier(random, op.random().alphabet(), op.random().minLength(), op.random().maxLength()));
            return ignored -> supplier.get();
        }
        else if (op.choose() != null) {
            Set<String> from = op.choose().stream().map(x -> (String) x).collect(Collectors.toSet());
            var supplier = Jackson.convertString(new ChooseStringSupplier(random, from));
            return ignored -> supplier.get();
        }
        else if (op.hmac() != null) {
            var fn = Jackson.convertString(new HmacStringFunction(key));
            return fn::apply;
        }
        else if (op.encrypt() != null) {
            var fn = Jackson.convertString(new EncryptStringFunction(key, random));
            return fn::apply;
        }
        else if (op.decrypt() != null) {
            var fn = Jackson.convertString(new DecryptStringFunction(key));
            return fn::apply;
        }
        else {
            return node -> node;
        }
    }

    private static JacksonFunction buildIntegerOp(ApplyConfig op, Random random) {
        if (op.value() != null) {
            var supplier = Jackson.convertInt(new ConstantIntSupplier(op.value().intValue()));
            return ignored -> supplier.get();
        }
        else if (op.random() != null) {
            var supplier = Jackson.convertInt(new RandomIntSupplier(random, op.random().min(), op.random().max()));
            return ignored -> supplier.get();
        }
        else if (op.choose() != null) {
            Set<Integer> from = op.choose().stream().map(x -> (Integer) x).collect(Collectors.toSet());
            var supplier = Jackson.convertInt(new ChooseIntSupplier(random, from));
            return ignored -> supplier.get();
        }
        else {
            return node -> node;
        }
    }
}
