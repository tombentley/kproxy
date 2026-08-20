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
import io.kroxylicious.filter.record.manipulation.config.MaskConfig;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A generator of {@link JsonNode}s, built from a {@link MaskConfig} tree.
 * <p>
 * This is declared as its own interface (rather than using {@code Supplier<JsonNode>} directly) so that
 * instances built by {@link #buildGenerator(MaskConfig)} carry a fixed, reflectable generic signature, the
 * same way {@link JacksonFunction} does.
 */
public interface JacksonSupplier extends Supplier<JsonNode> {

    /**
     * Builds a generator of {@link JsonNode}s from a {@link MaskConfig} tree.
     * @param maskTree the mask config tree
     * @return a supplier generating a new {@link JsonNode} matching the shape described by {@code maskTree}
     */
    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "The PRNG is deliberately not SecureRandom: we want control over "
            + "the seed (e.g. derived from topic/partition/offset) so masking can eventually have repeatable-read semantics on the Fetch path")
    static JacksonSupplier buildGenerator(MaskConfig maskTree) {
        return buildGenerator(maskTree, new Random());
    }

    private static JacksonSupplier buildGenerator(MaskConfig maskTree, Random random) {
        // TODO Key Mgmt
        switch (maskTree.type()) {
            // TODO "null", "boolean", "number"
            case "string" -> {
                if (maskTree.value() != null) {
                    var supplier = Jackson.convertString(new ConstantStringSupplier(maskTree.value().textValue()));
                    return supplier::get;
                }
                else if (maskTree.random() != null) {
                    var supplier = Jackson.convertString(
                            new RandomStringSupplier(random, maskTree.random().alphabet(), maskTree.random().minLength(), maskTree.random().maxLength()));
                    return supplier::get;
                }
                else if (maskTree.choose() != null) {
                    Set<String> from = maskTree.choose().stream().map(x -> (String) x).collect(Collectors.toSet());
                    var supplier = Jackson.convertString(new ChooseStringSupplier(random, from));
                    return supplier::get;
                }
                else {
                    return () -> new TextNode("");
                }
            }
            case "integer" -> {
                if (maskTree.value() != null) {
                    var supplier = Jackson.convertInt(new ConstantIntSupplier(maskTree.value().intValue()));
                    return supplier::get;
                }
                else if (maskTree.random() != null) {
                    var supplier = Jackson.convertInt(new RandomIntSupplier(random, maskTree.random().min(), maskTree.random().max()));
                    return supplier::get;
                }
                else if (maskTree.choose() != null) {
                    Set<Integer> from = maskTree.choose().stream().map(x -> (Integer) x).collect(Collectors.toSet());
                    var supplier = Jackson.convertInt(new ChooseIntSupplier(random, from));
                    return supplier::get;
                }
                else {
                    return () -> new IntNode(0);
                }
            }
            case "array" -> {
                if (maskTree.items() != null) {
                    var supplier = new ArrayNodes(JsonNodeFactory.instance).items2(buildGenerator(maskTree.items(), random));
                    return supplier::get;
                }
                else {
                    return () -> new ArrayNode(null);
                }
            }
            case "object" -> {
                if (maskTree.properties() != null) {
                    Map<String, JacksonSupplier> mapping = maskTree.properties().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> buildGenerator(e.getValue(), random)));
                    var supplier = new ObjectNodes(JsonNodeFactory.instance).mapProperties2(mapping);
                    return supplier::get;
                }
                else {
                    return () -> new ObjectNode(null);
                }
            }
            default -> throw new IllegalArgumentException("Invalid mask type: " + maskTree.type());
        }
    }
}
