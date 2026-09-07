/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.jackson;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.common.Maybe;

/**
 * TODO patternProperties (invocation order wrt properties)
 * TODO additionalProperties and unevaluatedPropertoes
 * TODO required (for generation, not for masking)
 * Not supported: propertyNames, minProperties, maxProperties
 * https://json-schema.org/understanding-json-schema/reference/object
 */
public class ObjectNodes {

    private final JsonNodeFactory nodeFactory;

    /**
     * Creates an instance.
     * @param nodeFactory the factory used to build result nodes
     */
    public ObjectNodes(JsonNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    /**
     * Maps selected properties of an object, and can also delete or insert properties. Serves both masking
     * (an object genuinely present in the data) and generation (an object node started from
     * {@link Maybe#none()} - see {@code JacksonFunction.buildStructural}'s object case) via the same
     * mechanism.
     * <p>
     * Builds a fresh object (the input is never mutated): each property present in the input is passed
     * through its mapped function if one exists in {@code map} (or carried over unchanged otherwise); each
     * key present in {@code map} but absent from the input is additionally invoked, with {@link Maybe#none()}
     * as its input, to support inserting a property that didn't previously exist. In both cases, a function
     * result of {@link Maybe#none()} is omitted from the result rather than written back - this is how a
     * function signals "delete this property" (if it was present) or "decline to insert" (if it wasn't).
     * <p>
     * Using {@link Maybe} here (rather than a {@code JsonNode} sentinel, as {@code JacksonFunction}'s own
     * {@code apply}-chain machinery does) means a function passed here cannot accidentally ignore absence the
     * way a naive {@code node -> node.asInt() + 1} could before - {@link Maybe} is sealed, so handling both
     * {@link Maybe.Some} and {@link Maybe.None} is enforced by the compiler when pattern-matched with
     * {@code switch}, rather than relying on documentation discipline alone.
     * <p>
     * This loop stays a single fused pass over {@code object}'s properties (building one fresh
     * {@link ObjectNode} directly) rather than composing {@code map}'s entries via
     * {@link io.kroxylicious.filter.record.manipulation.common.Property}/{@code JsonProperty} - each of those
     * would need its own copy of the accumulated object to stay non-mutating, making N independent
     * {@code Property.modify} calls quadratic in the property count where this stays linear, which matters
     * here since this runs once per record, not once at config-build time. {@code JsonProperty} still gives
     * exactly this method's semantics as a standalone, testable specification, cross-checked for agreement in
     * {@code JsonPropertyTest}.
     * @param map the per-property functions, keyed by property name
     * @return a function building a fresh object per the rules above
     */
    public BiFunction<ObjectNode, OpContext, ObjectNode> mapProperties(
                                                                     Map<String, ? extends BaseTypedOp<Maybe<JsonNode>, Maybe<JsonNode>>> map) {
        return new JsonNodePropertiesFunction(nodeFactory, map);
    }

    private record JsonNodePropertiesFunction(JsonNodeFactory nodeFactory,
                                              Map<String, ? extends BaseTypedOp<Maybe<JsonNode>, Maybe<JsonNode>>> propertyFns)
            implements BiFunction<ObjectNode, OpContext, ObjectNode> {

        @Override
        public ObjectNode apply(ObjectNode object, OpContext opContext) {
            // result.set(...) below mutates result directly, rather than going through Property.set (which
            // always defensively copies, precisely so it stays safe for arbitrary/possibly-aliased callers).
            // That's safe here specifically because result was *just* allocated on the line below and handed
            // to nobody else yet - a guarantee provable by inspection at this one call site, not a documented
            // caller contract. A "don't retain a reference after calling set" contract on Property.set itself
            // would not be safe to rely on here: object's own properties can be aliased by an ancestor several
            // recursion levels up (a nested object's ObjectNode is literally property.getValue() from its
            // parent's property map), so mutating a value "one level down" while it's still reachable from an
            // ancestor that never itself called set would silently corrupt that ancestor's view of its own
            // data, even though the ancestor never broke any rule itself.
            ObjectNode result = nodeFactory.objectNode();
            Set<String> handled = new HashSet<>();
            for (var property : object.properties()) {
                handled.add(property.getKey());
                var mapFn = propertyFns.get(property.getKey());
                Maybe<JsonNode> mapped = mapFn != null ? mapFn.apply(Maybe.some(property.getValue()), opContext) : Maybe.some(property.getValue());
                if (mapped instanceof Maybe.Some<JsonNode> some) {
                    result.set(property.getKey(), some.value());
                }
            }
            for (var entry : propertyFns.entrySet()) {
                if (handled.contains(entry.getKey())) {
                    continue;
                }
                Maybe<JsonNode> mapped = entry.getValue().apply(Maybe.none(), opContext);
                if (mapped instanceof Maybe.Some<JsonNode> some) {
                    result.set(entry.getKey(), some.value());
                }
            }
            return result;
        }
    }
}
