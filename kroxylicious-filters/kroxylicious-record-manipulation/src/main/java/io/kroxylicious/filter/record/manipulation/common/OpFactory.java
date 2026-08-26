/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * A plugin that builds an operation of a fixed, named shape (e.g. {@link StringOp}, {@link IntOp}) from
 * some configuration. Parameterised over the whole operation shape, rather than separately over input and
 * output types, so a future non-type-preserving operation (e.g. a hypothetical {@code String}-to-{@code
 * Integer} operation) fits the same pattern - it would just need its own fixed-generic operation interface
 * and a corresponding {@code OpFactory} subinterface, exactly like {@link StringOpFactory}/{@link
 * IntOpFactory} do today.
 * <p>
 * Deliberately narrower than the plugin config machinery used elsewhere in Kroxylicious
 * ({@code @PluginImplName}/{@code @PluginImplConfig}, which resolves a config's concrete type via Jackson
 * at parse time): which subinterface of {@code OpFactory} applies to a given configuration entry isn't
 * known until the format-specific engine walks its schema and discovers the field's type, so {@code
 * config} is left as an undeserialized property map here, and implementations convert it to their own
 * configuration type themselves. Deliberately typed with plain JDK types rather than a Jackson tree type
 * (e.g. {@code JsonNode}) so this interface - which every plugin implementor's method signature must
 * match - doesn't carry a {@code jackson-databind} dependency, only whatever a plugin's own {@code
 * ObjectMapper.convertValue} call needs internally; that keeps a future Jackson 3 migration (which is
 * expected to rename {@code databind}'s package, unlike {@code jackson-annotations}'s) from rippling
 * across every implementor.
 *
 * @param <Op> the operation type this factory builds
 */
public interface OpFactory<Op extends BiFunction<?, Context, ?>> {

    /**
     * Builds the operation described by the given configuration.
     * @param config the operation's configuration properties, not yet deserialized into a concrete type
     * @return the built operation
     */
    Op create(Map<String, Object> config);
}
