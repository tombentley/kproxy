/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.bootstrap;

import java.util.List;

public class MainPlugin implements Plugin {

    //public MainPlugin() {}

    private FooPlugin chosenFoo;
    private FooPlugin chosenFoo2;

    @Override
    public ConfigSchema configSchema(Plugins plugins) {
        return plugins.schemaBuilderFromString("""
                type: object
                properties:
                  num:
                    type: integer
                  foo: null
                  foo2: null
                required:
                - num
                - foo
                """)
                .replace(List.of("properties", "foo"), plugins.schemaForChoice(FooPlugin.class))
                .replace(List.of("properties", "foo2"), plugins.schemaForChoice(FooPlugin.class))
                .build();
    }

    public void configure(Plugins plugins, Config config) {
        this.chosenFoo = plugins.configureChosen(config, List.of("foo"), FooPlugin.class);
        this.chosenFoo2 = plugins.configureChosen(config, List.of("foo2"), FooPlugin.class);
    }

    @Override
    public String toString() {
        return "MainPlugin{" +
                "chosenFoo=" + chosenFoo +
                ", chosenFoo2=" + chosenFoo2 +
                '}';
    }
}
