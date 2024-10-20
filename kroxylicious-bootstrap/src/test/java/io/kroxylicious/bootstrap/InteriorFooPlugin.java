/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.bootstrap;

import java.util.List;
import java.util.Objects;

public class InteriorFooPlugin implements FooPlugin {

    private KlunkPlugin klunk;

    /**
     * A {@link FooService} implementation that itself depend on another service
     * (the {@link KlunkService}), making it an interior node in the tree of services.
     */
    static record InteriorFoo(KlunkService klunk) implements FooService {
        InteriorFoo {
            Objects.requireNonNull(klunk);
        }
    }
    @Override
    public FooService createFoo() {
        return new InteriorFoo(null);
    }

    @Override
    public ConfigSchema configSchema(Plugins plugins) {
        ConfigSchema.Builder configSchema = plugins.schemaBuilderFromString("""
                type: object
                properties:
                  klunk: null
                required:
                - klunk
                """);
        // now replace the value of the thing as properties.klunk with klunkImpls
        return configSchema
                .replace(List.of("properties", "klunk"), plugins.schemaForChoice(KlunkPlugin.class))
                .build();
    }

    @Override
    public void configure(
            Plugins plugins,
            Config config
    ) {
        this.klunk = plugins.configureChosen(config, List.of("klunk"), KlunkPlugin.class);
    }

    @Override
    public String toString() {
        return "InteriorFooPlugin{" +
                "klunk=" + klunk +
                '}';
    }
}
