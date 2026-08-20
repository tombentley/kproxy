/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.jackson;

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
import io.kroxylicious.filter.record.manipulation.common.RandomIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.RandomStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.Strings;
import io.kroxylicious.filter.record.manipulation.config.MaskConfig;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A mask/transform over a {@link JsonNode}, built from a {@link MaskConfig} tree.
 * <p>
 * This is declared as its own interface (rather than using {@code Function<JsonNode, JsonNode>} directly)
 * so that instances built by {@link #buildMask(MaskConfig)} carry a fixed, reflectable generic signature.
 * {@link io.kroxylicious.filter.record.manipulation.common.Pipeline} relies on this to check that a
 * {@code JacksonFunction} composes with the stage before it (e.g. a {@link JacksonDeserializer}).
 */
public interface JacksonFunction extends Function<JsonNode, JsonNode> {

    /**
     * Builds a mask function from a {@link MaskConfig} tree.
     * @param maskTree the mask config tree
     * @return a function transforming an input {@link JsonNode} according to {@code maskTree}
     */
    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "The PRNG is deliberately not SecureRandom: we want control over "
            + "the seed (e.g. derived from topic/partition/offset) so masking can eventually have repeatable-read semantics on the Fetch path")
    static JacksonFunction buildMask(MaskConfig maskTree) {
        return buildMask(maskTree, new Random());
    }

    private static JacksonFunction buildMask(MaskConfig maskTree, Random random) {
        byte[] key = new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };

        switch (maskTree.type()) {
            case "string" -> {
                if (maskTree.value() != null) {
                    var supplier = Jackson.convertString(new ConstantStringSupplier(maskTree.value().textValue()));
                    return ignored -> supplier.get();
                }
                else if (maskTree.random() != null) {
                    var supplier = Jackson.convertString(
                            new RandomStringSupplier(random, maskTree.random().alphabet(), maskTree.random().minLength(), maskTree.random().maxLength()));
                    return ignored -> supplier.get();
                }
                else if (maskTree.choose() != null) {
                    Set<String> from = maskTree.choose().stream().map(x -> (String) x).collect(Collectors.toSet());
                    var supplier = Jackson.convertString(new ChooseStringSupplier(random, from));
                    return ignored -> supplier.get();
                }
                else if (maskTree.hmac() != null) {
                    var fn = Jackson.convertString(new Strings(key, random).hmac());
                    return fn::apply;
                }
                else if (maskTree.encrypt() != null) {
                    var fn = Jackson.convertString(new Strings(key, random).encrypt());
                    return fn::apply;
                }
                else if (maskTree.decrypt() != null) {
                    var fn = Jackson.convertString(new Strings(key, random).decrypt());
                    return fn::apply;
                }
                else {
                    return node -> node;
                }
            }
            case "integer" -> {
                if (maskTree.value() != null) {
                    var supplier = Jackson.convertInt(new ConstantIntSupplier(maskTree.value().intValue()));
                    return ignored -> supplier.get();
                }
                else if (maskTree.random() != null) {
                    var supplier = Jackson.convertInt(new RandomIntSupplier(random, maskTree.random().min(), maskTree.random().max()));
                    return ignored -> supplier.get();
                }
                else if (maskTree.choose() != null) {
                    Set<Integer> from = maskTree.choose().stream().map(x -> (Integer) x).collect(Collectors.toSet());
                    var supplier = Jackson.convertInt(new ChooseIntSupplier(random, from));
                    return ignored -> supplier.get();
                }
                else {
                    return node -> node;
                }
            }
            case "array" -> {
                if (maskTree.items() != null) {
                    var fn = new ArrayNodes(JsonNodeFactory.instance).items(buildMask(maskTree.items(), random));
                    return node -> fn.apply((ArrayNode) node);
                }
                else {
                    return node -> node;
                }
            }
            case "object" -> {
                if (maskTree.properties() != null) {
                    Map<String, JacksonFunction> mapping = maskTree.properties().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> buildMask(e.getValue(), random)));
                    var fn = new ObjectNodes(JsonNodeFactory.instance).mapProperties(mapping);
                    return node -> fn.apply((ObjectNode) node);
                }
                else {
                    return node -> node;
                }
            }
            default -> throw new IllegalArgumentException("Invalid mask type: " + maskTree.type());
        }
    }
}
