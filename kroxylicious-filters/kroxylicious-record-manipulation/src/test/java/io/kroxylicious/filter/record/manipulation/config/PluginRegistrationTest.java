/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.common.BigIntegerOpFactory;
import io.kroxylicious.filter.record.manipulation.common.BooleanOpFactory;
import io.kroxylicious.filter.record.manipulation.common.BytesOpFactory;
import io.kroxylicious.filter.record.manipulation.common.DoubleOpFactory;
import io.kroxylicious.filter.record.manipulation.common.FloatOpFactory;
import io.kroxylicious.filter.record.manipulation.common.IntOpFactory;
import io.kroxylicious.filter.record.manipulation.common.LongOpFactory;
import io.kroxylicious.filter.record.manipulation.common.StringOpFactory;
import io.kroxylicious.proxy.config.PluginFactoryRegistry;
import io.kroxylicious.proxy.config.ServiceBasedPluginFactoryRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that every {@code Choose*}/{@code Random*}/{@code Value*}/{@code HmacString}/{@code EncryptString}/
 * {@code DecryptString} implementation of {@link StringOpFactory}, {@link IntOpFactory},
 * {@link LongOpFactory}, {@link DoubleOpFactory}, {@link FloatOpFactory}, {@link BooleanOpFactory},
 * {@link BytesOpFactory} and {@link BigIntegerOpFactory} is independently discoverable via
 * {@link java.util.ServiceLoader} through one uniform mechanism, and that plugin name resolution stays
 * correctly scoped per plugin interface even where two unrelated types happen to share an implementation's
 * simple name (e.g. {@code RandomInt} vs {@code RandomString}).
 * <p>
 * All eight plugin interfaces are treated identically here: {@code StringOpFactory}/{@code IntOpFactory}
 * aren't special - they're just the two plugin interfaces whose {@code META-INF/services} registration
 * happened to predate the other six.
 */
class PluginRegistrationTest {

    private static final PluginFactoryRegistry REGISTRY = new ServiceBasedPluginFactoryRegistry();

    @Test
    void randomIntIsRegisteredUnderIntOpFactory() {
        // Given/When
        IntOpFactory factory = REGISTRY.pluginFactory(IntOpFactory.class).pluginInstance("RandomInt");

        // Then
        assertThat(factory).isInstanceOf(RandomInt.class);
    }

    @Test
    void valueIntIsRegisteredUnderIntOpFactory() {
        // Given/When
        IntOpFactory factory = REGISTRY.pluginFactory(IntOpFactory.class).pluginInstance("ValueInt");

        // Then
        assertThat(factory).isInstanceOf(ValueInt.class);
    }

    @Test
    void chooseIntIsRegisteredUnderIntOpFactory() {
        // Given/When
        IntOpFactory factory = REGISTRY.pluginFactory(IntOpFactory.class).pluginInstance("ChooseInt");

        // Then
        assertThat(factory).isInstanceOf(ChooseInt.class);
    }

    @Test
    void randomStringIsRegisteredUnderStringOpFactory() {
        // Given/When
        StringOpFactory factory = REGISTRY.pluginFactory(StringOpFactory.class).pluginInstance("RandomString");

        // Then
        assertThat(factory).isInstanceOf(RandomString.class);
    }

    @Test
    void valueStringIsRegisteredUnderStringOpFactory() {
        // Given/When
        StringOpFactory factory = REGISTRY.pluginFactory(StringOpFactory.class).pluginInstance("ValueString");

        // Then
        assertThat(factory).isInstanceOf(ValueString.class);
    }

    @Test
    void chooseStringIsRegisteredUnderStringOpFactory() {
        // Given/When
        StringOpFactory factory = REGISTRY.pluginFactory(StringOpFactory.class).pluginInstance("ChooseString");

        // Then
        assertThat(factory).isInstanceOf(ChooseString.class);
    }

    @Test
    void hmacStringIsRegisteredUnderStringOpFactory() {
        // Given/When
        StringOpFactory factory = REGISTRY.pluginFactory(StringOpFactory.class).pluginInstance("HmacString");

        // Then
        assertThat(factory).isInstanceOf(HmacString.class);
    }

    @Test
    void encryptStringIsRegisteredUnderStringOpFactory() {
        // Given/When
        StringOpFactory factory = REGISTRY.pluginFactory(StringOpFactory.class).pluginInstance("EncryptString");

        // Then
        assertThat(factory).isInstanceOf(EncryptString.class);
    }

    @Test
    void decryptStringIsRegisteredUnderStringOpFactory() {
        // Given/When
        StringOpFactory factory = REGISTRY.pluginFactory(StringOpFactory.class).pluginInstance("DecryptString");

        // Then
        assertThat(factory).isInstanceOf(DecryptString.class);
    }

    @Test
    void chooseLongIsRegisteredUnderLongOpFactory() {
        // Given/When
        LongOpFactory factory = REGISTRY.pluginFactory(LongOpFactory.class).pluginInstance("ChooseLong");

        // Then
        assertThat(factory).isInstanceOf(ChooseLong.class);
    }

    @Test
    void randomLongIsRegisteredUnderLongOpFactory() {
        // Given/When
        LongOpFactory factory = REGISTRY.pluginFactory(LongOpFactory.class).pluginInstance("RandomLong");

        // Then
        assertThat(factory).isInstanceOf(RandomLong.class);
    }

    @Test
    void valueLongIsRegisteredUnderLongOpFactory() {
        // Given/When
        LongOpFactory factory = REGISTRY.pluginFactory(LongOpFactory.class).pluginInstance("ValueLong");

        // Then
        assertThat(factory).isInstanceOf(ValueLong.class);
    }

    @Test
    void chooseDoubleIsRegisteredUnderDoubleOpFactory() {
        // Given/When
        DoubleOpFactory factory = REGISTRY.pluginFactory(DoubleOpFactory.class).pluginInstance("ChooseDouble");

        // Then
        assertThat(factory).isInstanceOf(ChooseDouble.class);
    }

    @Test
    void randomDoubleIsRegisteredUnderDoubleOpFactory() {
        // Given/When
        DoubleOpFactory factory = REGISTRY.pluginFactory(DoubleOpFactory.class).pluginInstance("RandomDouble");

        // Then
        assertThat(factory).isInstanceOf(RandomDouble.class);
    }

    @Test
    void valueDoubleIsRegisteredUnderDoubleOpFactory() {
        // Given/When
        DoubleOpFactory factory = REGISTRY.pluginFactory(DoubleOpFactory.class).pluginInstance("ValueDouble");

        // Then
        assertThat(factory).isInstanceOf(ValueDouble.class);
    }

    @Test
    void chooseFloatIsRegisteredUnderFloatOpFactory() {
        // Given/When
        FloatOpFactory factory = REGISTRY.pluginFactory(FloatOpFactory.class).pluginInstance("ChooseFloat");

        // Then
        assertThat(factory).isInstanceOf(ChooseFloat.class);
    }

    @Test
    void randomFloatIsRegisteredUnderFloatOpFactory() {
        // Given/When
        FloatOpFactory factory = REGISTRY.pluginFactory(FloatOpFactory.class).pluginInstance("RandomFloat");

        // Then
        assertThat(factory).isInstanceOf(RandomFloat.class);
    }

    @Test
    void valueFloatIsRegisteredUnderFloatOpFactory() {
        // Given/When
        FloatOpFactory factory = REGISTRY.pluginFactory(FloatOpFactory.class).pluginInstance("ValueFloat");

        // Then
        assertThat(factory).isInstanceOf(ValueFloat.class);
    }

    @Test
    void randomBooleanIsRegisteredUnderBooleanOpFactory() {
        // Given/When
        BooleanOpFactory factory = REGISTRY.pluginFactory(BooleanOpFactory.class).pluginInstance("RandomBoolean");

        // Then
        assertThat(factory).isInstanceOf(RandomBoolean.class);
    }

    @Test
    void valueBooleanIsRegisteredUnderBooleanOpFactory() {
        // Given/When
        BooleanOpFactory factory = REGISTRY.pluginFactory(BooleanOpFactory.class).pluginInstance("ValueBoolean");

        // Then
        assertThat(factory).isInstanceOf(ValueBoolean.class);
    }

    @Test
    void randomBytesIsRegisteredUnderBytesOpFactory() {
        // Given/When
        BytesOpFactory factory = REGISTRY.pluginFactory(BytesOpFactory.class).pluginInstance("RandomBytes");

        // Then
        assertThat(factory).isInstanceOf(RandomBytes.class);
    }

    @Test
    void valueBytesIsRegisteredUnderBytesOpFactory() {
        // Given/When
        BytesOpFactory factory = REGISTRY.pluginFactory(BytesOpFactory.class).pluginInstance("ValueBytes");

        // Then
        assertThat(factory).isInstanceOf(ValueBytes.class);
    }

    @Test
    void chooseBigIntegerIsRegisteredUnderBigIntegerOpFactory() {
        // Given/When
        BigIntegerOpFactory factory = REGISTRY.pluginFactory(BigIntegerOpFactory.class).pluginInstance("ChooseBigInteger");

        // Then
        assertThat(factory).isInstanceOf(ChooseBigInteger.class);
    }

    @Test
    void valueBigIntegerIsRegisteredUnderBigIntegerOpFactory() {
        // Given/When
        BigIntegerOpFactory factory = REGISTRY.pluginFactory(BigIntegerOpFactory.class).pluginInstance("ValueBigInteger");

        // Then
        assertThat(factory).isInstanceOf(ValueBigInteger.class);
    }

}
