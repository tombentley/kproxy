/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.kroxylicious.filter.record.manipulation.common.ChooseIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.ChooseStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.ConstantIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.ConstantStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.RandomIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.RandomStringSupplier;
import io.kroxylicious.filter.record.manipulation.config.ApplyConfig;
import io.kroxylicious.filter.record.manipulation.config.SchemaConfig;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A generator of {@link JsonNode}s, built from a {@link SchemaConfig} tree.
 * <p>
 * This is declared as its own interface (rather than using {@code Supplier<JsonNode>} directly) so that
 * instances built by {@link #buildGenerator(SchemaConfig)} carry a fixed, reflectable generic signature, the
 * same way {@link JacksonFunction} does.
 * <p>
 * Unlike {@link JacksonFunction#buildMask(SchemaConfig)}, only the first entry of a node's {@code apply} list
 * is used here: there is no prior value to feed forward when generating from nothing, so composing multiple
 * operations during generation isn't supported yet.
 */
public interface JacksonSupplier extends Supplier<JsonNode> {

    /**
     * Builds a generator of {@link JsonNode}s from a {@link SchemaConfig} tree.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @return a supplier generating a new {@link JsonNode} matching the shape described by {@code schema}
     */
    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "The PRNG is deliberately not SecureRandom: we want control over "
            + "the seed (e.g. derived from topic/partition/offset) so masking can eventually have repeatable-read semantics on the Fetch path")
    static JacksonSupplier buildGenerator(SchemaConfig schema) {
        return buildGenerator(schema, new Random());
    }

    /**
     * Builds a generator of {@link JsonNode}s from a {@link SchemaConfig} tree, drawing all randomness from
     * the given {@code random} instance, threaded through every recursive call.
     * @param schema the schema tree, annotated with {@code apply} chains
     * @param random the source of randomness for this schema tree and all its nested generators
     * @return a supplier generating a new {@link JsonNode} matching the shape described by {@code schema}
     */
    static JacksonSupplier buildGenerator(SchemaConfig schema, Random random) {
        // TODO Key Mgmt
        if (schema.apply() != null) {
            return buildOp(schema.type(), schema.apply().get(0), random);
        }
        switch (schema.type()) {
            // TODO "null", "boolean", "number"
            case "string" -> {
                return () -> new TextNode("");
            }
            case "integer" -> {
                return () -> new IntNode(0);
            }
            case "array" -> {
                if (schema.items() != null) {
                    var supplier = new ArrayNodes(JsonNodeFactory.instance).items2(buildGenerator(schema.items(), random));
                    return supplier::get;
                }
                else {
                    return () -> new ArrayNode(null);
                }
            }
            case "object" -> {
                if (schema.properties() != null) {
                    Map<String, JacksonSupplier> mapping = schema.properties().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> buildGenerator(e.getValue(), random)));
                    var supplier = new ObjectNodes(JsonNodeFactory.instance).mapProperties2(mapping);
                    return supplier::get;
                }
                else {
                    return () -> new ObjectNode(null);
                }
            }
            default -> throw new IllegalArgumentException("Invalid mask type: " + schema.type());
        }
    }

    private static JacksonSupplier buildOp(String type, ApplyConfig op, Random random) {
        return switch (type) {
            case "string" -> buildStringOp(op, random);
            case "integer" -> buildIntegerOp(op, random);
            default -> throw new IllegalArgumentException("apply is not yet supported for type " + type);
        };
    }

    private static JacksonSupplier buildStringOp(ApplyConfig op, Random random) {
        if (op.value() != null) {
            var supplier = Jackson.convertString(new ConstantStringSupplier(op.value().textValue()));
            return supplier::get;
        }
        else if (op.random() != null) {
            var supplier = Jackson.convertString(
                    new RandomStringSupplier(random, op.random().alphabet(), op.random().minLength(), op.random().maxLength()));
            return supplier::get;
        }
        else if (op.choose() != null) {
            Set<String> from = op.choose().stream().map(x -> (String) x).collect(Collectors.toSet());
            var supplier = Jackson.convertString(new ChooseStringSupplier(random, from));
            return supplier::get;
        }
        else {
            return () -> new TextNode("");
        }
    }

    private static JacksonSupplier buildIntegerOp(ApplyConfig op, Random random) {
        if (op.value() != null) {
            var supplier = Jackson.convertInt(new ConstantIntSupplier(op.value().intValue()));
            return supplier::get;
        }
        else if (op.random() != null) {
            var supplier = Jackson.convertInt(new RandomIntSupplier(random, op.random().min(), op.random().max()));
            return supplier::get;
        }
        else if (op.choose() != null) {
            Set<Integer> from = op.choose().stream().map(x -> (Integer) x).collect(Collectors.toSet());
            var supplier = Jackson.convertInt(new ChooseIntSupplier(random, from));
            return supplier::get;
        }
        else {
            return () -> new IntNode(0);
        }
    }
}
