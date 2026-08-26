/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.common.IntOpFactory;
import io.kroxylicious.filter.record.manipulation.common.StringOpFactory;
import io.kroxylicious.proxy.config.PluginFactoryRegistry;
import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that {@link RandomInt} and {@link RandomString} are independently discoverable via
 * {@link java.util.ServiceLoader}, each under its own plugin interface - Kroxylicious's plugin name
 * resolution is scoped per plugin interface, so this works even though both classes are operations named
 * "Random" for two different primitive types.
 */
class RandomPluginRegistrationTest {

    private static final PluginFactoryRegistry REGISTRY = new ServiceBasedPluginFactoryRegistry();

    @Test
    void randomIntIsRegisteredUnderIntOpFactory() {
        // Given/When
        IntOpFactory factory = REGISTRY.pluginFactory(IntOpFactory.class).pluginInstance("RandomInt");

        // Then
        assertThat(factory).isInstanceOf(RandomInt.class);
    }

    @Test
    void randomStringIsRegisteredUnderStringOpFactory() {
        // Given/When
        StringOpFactory factory = REGISTRY.pluginFactory(StringOpFactory.class).pluginInstance("RandomString");

        // Then
        assertThat(factory).isInstanceOf(RandomString.class);
    }

}
