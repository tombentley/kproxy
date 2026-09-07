/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.leangen.geantyref.TypeToken;

import io.kroxylicious.filter.record.manipulation.op.BaseTypedOp;
import io.kroxylicious.filter.record.manipulation.op.OpFactory;
import io.kroxylicious.proxy.config.PluginFactoryRegistry;
import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that every {@code Choose*}/{@code Random*}/{@code Value*}/{@code HmacString}/{@code EncryptString}/
 * {@code DecryptString} implementation of {@link OpFactory} is independently discoverable, by its own
 * unique plugin name, via {@link java.util.ServiceLoader} - even though every plugin now shares the same
 * single {@link OpFactory} interface rather than being partitioned into a per-base-type interface (e.g. a
 * former, now-deleted {@code IntOpFactory}/{@code StringOpFactory}). Plugin name resolution stays correctly
 * scoped even where two unrelated types happen to share an implementation's simple name (e.g. {@code
 * RandomInt} vs {@code RandomString}).
 */
class PluginRegistrationTest {

    private static final PluginFactoryRegistry REGISTRY = new ServiceBasedPluginFactoryRegistry();

    @Test
    void randomIntIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("RandomInt");

        // Then
        assertThat(factory).isInstanceOf(RandomInt.class);
    }

    @Test
    void valueIntIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ValueInt");

        // Then
        assertThat(factory).isInstanceOf(ValueInt.class);
    }

    @Test
    void chooseIntIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ChooseInt");

        // Then
        assertThat(factory).isInstanceOf(ChooseInt.class);
    }

    @Test
    void randomStringIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("RandomString");

        // Then
        assertThat(factory).isInstanceOf(RandomString.class);
    }

    @Test
    void valueStringIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ValueString");

        // Then
        assertThat(factory).isInstanceOf(ValueString.class);
    }

    @Test
    void chooseStringIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ChooseString");

        // Then
        assertThat(factory).isInstanceOf(ChooseString.class);
    }

    @Test
    void hmacStringIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("HmacString");

        // Then
        assertThat(factory).isInstanceOf(HmacString.class);
    }

    @Test
    void encryptStringIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("EncryptString");

        // Then
        assertThat(factory).isInstanceOf(EncryptString.class);
    }

    @Test
    void decryptStringIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("DecryptString");

        // Then
        assertThat(factory).isInstanceOf(DecryptString.class);
    }

    @Test
    void chooseLongIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ChooseLong");

        // Then
        assertThat(factory).isInstanceOf(ChooseLong.class);
    }

    @Test
    void randomLongIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("RandomLong");

        // Then
        assertThat(factory).isInstanceOf(RandomLong.class);
    }

    @Test
    void valueLongIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ValueLong");

        // Then
        assertThat(factory).isInstanceOf(ValueLong.class);
    }

    @Test
    void chooseDoubleIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ChooseDouble");

        // Then
        assertThat(factory).isInstanceOf(ChooseDouble.class);
    }

    @Test
    void randomDoubleIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("RandomDouble");

        // Then
        assertThat(factory).isInstanceOf(RandomDouble.class);
    }

    @Test
    void valueDoubleIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ValueDouble");

        // Then
        assertThat(factory).isInstanceOf(ValueDouble.class);
    }

    @Test
    void chooseFloatIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ChooseFloat");

        // Then
        assertThat(factory).isInstanceOf(ChooseFloat.class);
    }

    @Test
    void randomFloatIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("RandomFloat");

        // Then
        assertThat(factory).isInstanceOf(RandomFloat.class);
    }

    @Test
    void valueFloatIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ValueFloat");

        // Then
        assertThat(factory).isInstanceOf(ValueFloat.class);
    }

    @Test
    void randomBooleanIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("RandomBoolean");

        // Then
        assertThat(factory).isInstanceOf(RandomBoolean.class);
    }

    @Test
    void valueBooleanIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ValueBoolean");

        // Then
        assertThat(factory).isInstanceOf(ValueBoolean.class);
    }

    @Test
    void randomBytesIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("RandomBytes");

        // Then
        assertThat(factory).isInstanceOf(RandomBytes.class);
    }

    @Test
    void valueBytesIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ValueBytes");

        // Then
        assertThat(factory).isInstanceOf(ValueBytes.class);
    }

    @Test
    void chooseBigIntegerIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ChooseBigInteger");

        // Then
        assertThat(factory).isInstanceOf(ChooseBigInteger.class);
    }

    @Test
    void valueBigIntegerIsRegisteredUnderOpFactory() {
        // Given/When
        OpFactory<?, ?> factory = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("ValueBigInteger");

        // Then
        assertThat(factory).isInstanceOf(ValueBigInteger.class);
    }

    @Test
    void resolvedFactoriesBuildOperationsWithTheExpectedInputAndOutputTypes() {
        // Given
        OpFactory<?, ?> randomInt = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("RandomInt");
        OpFactory<?, ?> randomString = REGISTRY.pluginFactory(OpFactory.class).pluginInstance("RandomString");

        // When
        BaseTypedOp<?, ?> intOp = randomInt.create(Map.of("minInclusive", 0, "maxExclusive", 10), null, null);
        BaseTypedOp<?, ?> stringOp = randomString.create(Map.of("alphabet", "abc", "minLengthInclusive", 1, "maxLengthExclusive", 5), null, null);

        // Then
        assertThat(intOp.inputType()).isEqualTo(TypeToken.get(Integer.class));
        assertThat(intOp.outputType()).isEqualTo(TypeToken.get(Integer.class));
        assertThat(stringOp.inputType()).isEqualTo(TypeToken.get(String.class));
        assertThat(stringOp.outputType()).isEqualTo(TypeToken.get(String.class));
    }

}
