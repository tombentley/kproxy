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

    @Override
    public ConfigSchema configSchema(Plugins plugins) {
        return plugins.schemaTemplateFromString("""
                type: object
                properties:
                  num:
                    type: integer
                  foo: null
                required:
                - num
                - foo
                """)
                .replace(List.of("properties", "foo"), plugins.schemaForChoice(FooPlugin.class))
                .build();
    }

    public void configure(Plugins plugins, Config config) {
        this.chosenFoo = plugins.configureChosen(config, List.of("foo"), FooPlugin.class);
    }

    @Override
    public String toString() {
        return "MainPlugin{" +
                "chosenFoo=" + chosenFoo +
                '}';
    }
}
