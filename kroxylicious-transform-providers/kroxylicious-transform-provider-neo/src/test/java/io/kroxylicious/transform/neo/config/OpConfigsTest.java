/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.neo.config;

import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;
import io.kroxylicious.proxy.plugin.UnknownPluginInstanceException;
import io.kroxylicious.transform.neo.RecordTimestamp;
import io.kroxylicious.transform.neo.RecordValue;
import io.kroxylicious.transform.neo.format.jackson2.JsonTransform;
import io.kroxylicious.transform.neo.format.jackson2.ReadJson;
import io.kroxylicious.transform.neo.format.jackson2.WriteJson;
import io.kroxylicious.transform.neo.op.OpConfig;
import io.kroxylicious.transform.neo.op.OpConfigs;
import io.kroxylicious.transform.neo.op.OpContext;
import io.kroxylicious.transform.neo.op.TypeException;
import io.kroxylicious.transform.neo.ops.common.Requirement;
import io.kroxylicious.transform.neo.ops.constant.ValueInt;
import io.kroxylicious.transform.records.RecordView;
import io.kroxylicious.transform.service.TransformInitializationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpConfigsTest {

    OpContext opContext = new OpContext(new Random(), new byte[0]);
    ServiceBasedPluginFactoryRegistry registry = new ServiceBasedPluginFactoryRegistry();

    TransformInitializationContext lookup = new TransformInitializationContext() {
        @Override
        public <S> S serviceClient(Class<S> serviceClass, String instanceName) {
            return null;
        }

        @Override
        public <P> P pluginInstance(Class<P> pluginClass, String implementationName) {
            return registry.pluginFactory(pluginClass).pluginInstance(implementationName);
        }
    };

    @Test
    void simplePipeline() {
        var pipe = OpConfigs.compose(RecordView.class, List.of(
                new OpConfig(RecordValue.class),
                new OpConfig(ReadJson.class),
                new OpConfig(JsonTransform.class,
                        Map.of(JsonTransform.SCHEMA_PARAMETER,
                                Map.of("type", "integer",
                                        "apply", List.of(
                                                Map.of(
                                                        "op", ValueInt.class.getName(),
                                                        "value", 999))))),
                new OpConfig(WriteJson.class)), Set.of(), lookup);

        assertThat(pipe.inputType()).isEqualTo(RecordView.class);
        assertThat(pipe.outputType()).isEqualTo(ByteBuffer.class);
        var record = mock(RecordView.class);
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
                .hasMessage("Op io.kroxylicious.transform.neo.RecordValue has input type "
                        + "io.kroxylicious.transform.records.RecordView which is not a subtype of "
                        + "the expected type java.lang.Integer");
    }

    @Test
    void typeCheckingDetectsOpsThatDoNotCompose() {
        List<OpConfig> opConfigs = List.of(
                new OpConfig(RecordTimestamp.class), // returns Long
                new OpConfig(ReadJson.class) // expects ByteBuffer
        );
        Set<Requirement> requirements = Set.of();
        assertThatThrownBy(() -> OpConfigs.compose(RecordView.class, opConfigs, requirements, lookup))
                .isInstanceOf(TypeException.class)
                .hasMessage("Op " + ReadJson.class.getName() + " has input type "
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
                new OpConfig(ReadJson.class, Map.of("thisParameterDoesNotExist", 999)));
        Set<Requirement> requirements = Set.of();
        assertThatThrownBy(() -> OpConfigs.compose(ByteBuffer.class, opConfigs, requirements, lookup))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unrecognized field \"thisParameterDoesNotExist\"");
    }

}
