/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.bootstrap;

public class LeafKlunkPlugin implements KlunkPlugin {

    private String name;

    static class LeafKlunkService implements KlunkService {

    }
    @Override
    public KlunkService createKunk() {
        return new LeafKlunkService();
    }

    @Override
    public ConfigSchema configSchema(Plugins plugins) {
        return plugins.schemaFromString("""
                type: object
                properties:
                  name:
                    type: string
                required:
                - name
                """);
    }

    @Override
    public void configure(
            Plugins plugins,
            Config config
    ) {
        this.name = config.toJsonNode().get("name").textValue();
    }

    @Override
    public String toString() {
        return "LeafKlunkPlugin{" +
                "name='" + name + '\'' +
                '}';
    }
}
