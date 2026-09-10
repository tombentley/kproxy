/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.kafka.common.record.Record;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.Requirement;
import io.kroxylicious.filter.record.manipulation.filter.RecordTimestamp;
import io.kroxylicious.filter.record.manipulation.filter.RecordValue;
import io.kroxylicious.filter.record.manipulation.format.jackson2.DeserializeJson;
import io.kroxylicious.filter.record.manipulation.format.jackson2.JsonTransform;
import io.kroxylicious.filter.record.manipulation.format.jackson2.SerializeJson;
import io.kroxylicious.filter.record.manipulation.op.OpConfig;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.TypeException;
import io.kroxylicious.filter.record.manipulation.ops.constant.ValueInt;
import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;
import io.kroxylicious.proxy.plugin.UnknownPluginInstanceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpConfigsTest {

    OpContext opContext = new OpContext(new Random(), new byte[0]);
    ServiceBasedPluginFactoryRegistry registry = new ServiceBasedPluginFactoryRegistry();

    PluginLookup lookup = new PluginLookup() {
        @Override
        public <P> P pluginInstance(Class<P> pluginClass, String implementationName) {
            return registry.pluginFactory(pluginClass).pluginInstance(implementationName);
        }
    };

    @Test
    void simplePipeline() {
        var pipe = OpConfigs.compose(Record.class, List.of(
                new OpConfig(RecordValue.class),
                new OpConfig(DeserializeJson.class),
                new OpConfig(JsonTransform.class,
                        Map.of("schema",
                                Map.of("type", "integer",
                                        "apply", List.of(
                                                Map.of(
                                                        "op", ValueInt.class.getName(),
                                                        "value", 999))))),
                new OpConfig(SerializeJson.class)), Set.of(), lookup);

        assertThat(pipe.inputType()).isEqualTo(Record.class);
        assertThat(pipe.outputType()).isEqualTo(ByteBuffer.class);
        var record = mock(Record.class);
        when(record.value()).thenReturn(ByteBuffer.wrap("12".getBytes(StandardCharsets.UTF_8)));
        assertThat(pipe.apply(record, opContext)).isEqualTo(ByteBuffer.wrap("999".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void emptyPipeline() {
        var pipe = OpConfigs.compose(Record.class, List.of(), Set.of(), lookup);

        assertThat(pipe.inputType()).isInstanceOf(TypeVariable.class).hasToString("T");
        assertThat(pipe.outputType()).isInstanceOf(TypeVariable.class).hasToString("T");
        var record = mock(Record.class);
        assertThat(pipe.apply(record, opContext)).isEqualTo(record);
        Mockito.verifyNoInteractions(record);
    }

    @Test
    void typeCheckingDetectsIncompatibleExpectedType() {
        List<OpConfig> opConfigs = List.of(
                new OpConfig(RecordValue.class) // expects Record
        );
        Set<Requirement> requirements = Set.of();
        Class<Integer> expectedType = Integer.class; // caller expects an Op that accepts an Integer
        assertThatThrownBy(() -> OpConfigs.compose(expectedType, opConfigs, requirements, lookup))
                .isInstanceOf(TypeException.class)
                .hasMessage("Op io.kroxylicious.filter.record.manipulation.filter.RecordValue has input type "
                        + "org.apache.kafka.common.record.Record which is not a subtype of "
                        + "the expected type java.lang.Integer");
    }

    @Test
    void typeCheckingDetectsOpsThatDoNotCompose() {
        List<OpConfig> opConfigs = List.of(
                new OpConfig(RecordTimestamp.class), // returns Long
                new OpConfig(DeserializeJson.class) // expects ByteBuffer
        );
        Set<Requirement> requirements = Set.of();
        assertThatThrownBy(() -> OpConfigs.compose(Record.class, opConfigs, requirements, lookup))
                .isInstanceOf(TypeException.class)
                .hasMessage("Op io.kroxylicious.filter.record.manipulation.format.jackson.DeserializeJson has input type "
                        + "java.nio.ByteBuffer which is not a subtype of "
                        + "the expected type java.lang.Long");
    }

    @Test
    void throwsIfPluginLookupFails() {
        List<OpConfig> opConfigs = List.of(
                new OpConfig("this.type.does.not.Exist", Map.of()));
        Set<Requirement> requirements = Set.of();
        assertThatThrownBy(() -> OpConfigs.compose(Record.class, opConfigs, requirements, lookup))
                .isInstanceOf(UnknownPluginInstanceException.class)
                .hasMessageContaining("this.type.does.not.Exist");
    }

    @Test
    void throwsIfConcompatibleConfig() {
        List<OpConfig> opConfigs = List.of(
                new OpConfig(DeserializeJson.class, Map.of("thisParameterDoesNotExist", 999)));
        Set<Requirement> requirements = Set.of();
        assertThatThrownBy(() -> OpConfigs.compose(ByteBuffer.class, opConfigs, requirements, lookup))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unrecognized field \"thisParameterDoesNotExist\"");
    }

}
