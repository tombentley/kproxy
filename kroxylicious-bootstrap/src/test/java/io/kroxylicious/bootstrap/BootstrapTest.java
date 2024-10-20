/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.bootstrap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BootstrapTest {

    @Test
    void shouldThrowOnInvalidConfig() {
        String configContent = """
                foo: 
                  InteriorFooPlugin: 
                    klunk:
                      LeafKlunkPlugin:
                        name: bob""";
        Bootstrap bootstrap = new Bootstrap();
        assertThatThrownBy(() -> bootstrap.start(MainPlugin.class, configContent)).hasMessage(
                "Invalid config: [$: required property 'num' not found]");
    }

    @Test
    void shouldThrowOnInvalidLeafConfig() {
        String configContent = """
                num: 1
                foo: 
                  InteriorFooPlugin: 
                    klunk:
                      LeafKlunkPlugin:
                        name: null""";
        Bootstrap bootstrap = new Bootstrap();
        assertThatThrownBy(() -> bootstrap.start(MainPlugin.class, configContent)).hasMessage(
                "Invalid config: [$.foo.InteriorFooPlugin.klunk.LeafKlunkPlugin.name: null found, string expected]");
    }

    @Test
    void useSite() {
        String configContent = """
                num: 1
                foo: 
                  InteriorFooPlugin: 
                    klunk:
                      LeafKlunkPlugin:
                        name: bob""";
        Bootstrap bootstrap = new Bootstrap();
        var mainPlugin = bootstrap.start(MainPlugin.class, configContent);

        assertThat(mainPlugin).hasToString(
                "MainPlugin{chosenFoo=InteriorFooPlugin{klunk=LeafKlunkPlugin{name='bob'}}}");

    }

    @Test
    void useSite2() {
        String configContent = """
                num: 1
                foo: 
                  InteriorFooPlugin: 
                    klunk:
                      LeafKlunkPlugin:
                        name: bob
                foo2: 
                  InteriorFooPlugin: 
                    klunk:
                      LeafKlunkPlugin:
                        name: brenda""";
        Bootstrap bootstrap = new Bootstrap();
        var mainPlugin = bootstrap.start(MainPlugin.class, configContent);

        assertThat(mainPlugin).hasToString(
                "MainPlugin{chosenFoo=InteriorFooPlugin{klunk=LeafKlunkPlugin{name='bob'}}, chosenFoo2=InteriorFooPlugin{klunk=LeafKlunkPlugin{name='brenda'}}}");

    }

}