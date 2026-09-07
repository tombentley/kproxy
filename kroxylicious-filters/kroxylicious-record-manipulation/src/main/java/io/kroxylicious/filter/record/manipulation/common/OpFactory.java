/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * A plugin that builds a {@link TypedOp} from {@code T} to {@code R} from some configuration.
 * Parameterised over input and output type directly, rather than over a fixed, named operation shape
 * (contrast the pre-{@link TypedOp} design, which needed a bespoke marker interface like a hypothetical
 * {@code StringOp}/{@code IntOp} plus a matching {@code OpFactory} subinterface for every base type): a
 * plugin for a new base type, or a future non-type-preserving operation (e.g. {@code String}-to-{@code
 * Integer}), is just a new {@code OpFactory<T, R>} implementation - no new interface declaration needed.
 * <p>
 * Every implementation is registered under this one interface via {@link java.util.ServiceLoader}, so
 * plugin names are unique across every operation, not partitioned per base type - see {@link
 * io.kroxylicious.filter.record.manipulation.config.OpConfigs#resolveOp} for how a resolved instance's
 * {@link TypedOp#inputType()}/{@link TypedOp#outputType()} are checked against what the caller expected.
 * <p>
 * Deliberately narrower than the plugin config machinery used elsewhere in Kroxylicious
 * ({@code @PluginImplName}/{@code @PluginImplConfig}, which resolves a config's concrete type via Jackson
 * at parse time): which input/output type a given configuration entry should resolve to isn't known
 * until the format-specific engine walks its schema and discovers the field's type, so {@code
 * config} is left as an undeserialized property map here, and implementations convert it to their own
 * configuration type themselves. Deliberately typed with plain JDK types rather than a Jackson tree type
 * (e.g. {@code JsonNode}) so this interface - which every plugin implementor's method signature must
 * match - doesn't carry a {@code jackson-databind} dependency, only whatever a plugin's own {@code
 * ObjectMapper.convertValue} call needs internally; that keeps a future Jackson 3 migration (which is
 * expected to rename {@code databind}'s package, unlike {@code jackson-annotations}'s) from rippling
 * across every implementor.
 *
 * @param <T> the input type of the operation this factory builds
 * @param <R> the output type of the operation this factory builds
 */
public interface OpFactory<T, R> {

    /**
     * Builds the operation described by the given configuration.
     *
     * @param config the operation's configuration properties, not yet deserialized into a concrete type
     * @param lookup The lookup
     * @param argumentType The argument type
     * @return the built operation
     */
    BaseTypedOp<T, R> create(Map<String, Object> config, PluginLookup lookup, Type argumentType);
}
