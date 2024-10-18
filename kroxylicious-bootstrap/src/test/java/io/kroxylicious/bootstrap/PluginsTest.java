/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.bootstrap;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PluginsTest {

    @Test
    void schemaForChoice() {
        var plugins = new Plugins(List.of(new LeafKlunkPlugin(), new InteriorFooPlugin()));
        var z = plugins.schemaForChoice(KlunkPlugin.class);
        assertThat(z).hasToString("""
                  type: "object"
                  minProperties: 1
                  maxProperties: 1
                  properties:
                    LeafKlunkPlugin:
                      type: "object"
                      properties:
                        name:
                          type: "string"
                      required:
                      - "name"
                  """);
    }

}