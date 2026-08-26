/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

/**
 * Resolves a named plugin implementation of a given plugin interface. Shaped like
 * {@code io.kroxylicious.proxy.filter.FilterFactoryContext#pluginInstance(Class, String)} so that once
 * this module is wired into a real {@code Filter}, callers can switch to that method with no signature
 * change - this module doesn't otherwise depend on {@code kroxylicious-runtime}, where the concrete
 * plugin registry lives.
 */
@FunctionalInterface
public interface PluginLookup {

    /**
     * Gets a plugin instance for the given plugin type and name.
     * @param pluginClass the plugin type
     * @param implementationName the plugin implementation name
     * @return the plugin instance
     * @param <P> the plugin type
     */
    <P> P pluginInstance(Class<P> pluginClass, String implementationName);
}
