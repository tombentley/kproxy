/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.bootstrap;

/**
 * Baseclass for plugins.
 */
public interface Plugin {
    /**
     * Get the configuration schema for this plugin
     */
    ConfigSchema configSchema(Plugins plugins);

    /**
     * Configure this plugin using the given config, which is
     * guaranteed to be valid according to the schema returned by
     * {@link #configSchema(Plugins)}.
     * @param plugins
     * @param config
     */
    void configure(Plugins plugins, Config config);
}