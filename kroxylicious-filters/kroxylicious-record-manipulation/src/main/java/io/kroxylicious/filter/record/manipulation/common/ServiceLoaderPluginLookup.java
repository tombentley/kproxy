/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.ServiceLoader;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;

/**
 * A {@link PluginLookup} backed directly by the JDK's {@link ServiceLoader}, matching a plugin
 * implementation by its simple class name. Deliberately independent of Kroxylicious's own
 * {@code ServiceBasedPluginFactoryRegistry} (which lives in {@code kroxylicious-runtime}) so that
 * {@code main()}-based demos in this module - which aren't wired into a real {@code Filter} yet, and so
 * have nowhere else to source a {@link PluginLookup} from - don't need this module to take on a
 * {@code kroxylicious-runtime} dependency just to run.
 */
public class ServiceLoaderPluginLookup implements PluginLookup {

    @Override
    public <P> P pluginInstance(Class<P> pluginClass, String implementationName) {
        return ServiceLoader.load(pluginClass).stream()
                .filter(provider -> provider.type().getSimpleName().equals(implementationName))
                .findFirst()
                .map(ServiceLoader.Provider::get)
                .orElseThrow(() -> new IllegalArgumentException("No " + pluginClass.getSimpleName() + " implementation named '" + implementationName + "'"));
    }
}
