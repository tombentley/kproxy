/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.bootstrap;

/**
 * Baseclass for plugins, which provide
 */
public interface Plugin {
    /** Get the configuration schema for this plugin */
    ConfigSchema configSchema(Plugins plugins);




    void configure(Plugins plugins, Config config);

}