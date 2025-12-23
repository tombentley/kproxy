/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.kafka.common.header.Header;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DatumTransformationTest {

    static List<Arguments> shouldCheckConstructorArguments() {
        return List.of(
                Arguments.of(
                        Optional.empty(),
                        List.of(Integer.class, Integer.class)),
                Arguments.of(
                        Optional.empty(),
                        List.of(Integer.class, Object.class)),
                Arguments.of(
                        Optional.of("The serializer of type SER cannot accept values of type java.lang.Integer returned from the deserializer of type DESER"),
                        List.of(Integer.class, String.class)),
                Arguments.of(
                        Optional.empty(),
                        List.of(Integer.class, Integer.class, String.class, String.class)),
                Arguments.of(
                        Optional.empty(),
                        List.of(Integer.class, Object.class, String.class, String.class)),
                Arguments.of(
                        Optional.empty(),
                        List.of(Integer.class, Object.class, String.class, Object.class)),
                Arguments.of(
                        Optional.of("The mapper of type MAPPER0 cannot accept values of type java.lang.Integer returned from the deserializer of type DESER"),
                        List.of(Integer.class, String.class, String.class, String.class)),
                Arguments.of(
                        Optional.of("The serializer of type SER cannot accept values of type java.lang.Integer returned from the mapper of type MAPPER0"),
                        List.of(Integer.class, Integer.class, Integer.class, String.class)),
                Arguments.of(
                        Optional.of("The mapper of type MAPPER1 cannot accept values of type java.lang.Integer returned from the mapper of type MAPPER0"),
                        List.of(Integer.class, Integer.class, Integer.class, String.class, String.class, String.class))
        );
    }

    @ParameterizedTest
    @MethodSource
    void shouldCheckConstructorArguments(Optional<String> expectedMessage, List<Class<?>> types) {
        // Given
        var dd = mock(DatumDeserializer.class);
        Mockito.when(dd.returnedType()).thenReturn(types.get(0));
        expectedMessage = expectedMessage.map(s -> s.replace("DESER", dd.getClass().getName()));

        List<DatumMapper<?, ?>> mappers = new ArrayList<>();
        for (int i = 1; i < types.size() - 1; i+=2) {
            var dm = mock(DatumMapper.class);
            Mockito.when(dm.acceptedType()).thenReturn(types.get(i));
            Mockito.when(dm.returnedType()).thenReturn(types.get(i+1));
            mappers.add(dm);
            String target = "MAPPER" + ((i - 1) / 2);
            expectedMessage = expectedMessage.map(s -> s.replace(target, dm.getClass().getName()));
        }

        var ds = mock(DatumSerializer.class);
        Mockito.when(ds.acceptedType()).thenReturn(types.get(types.size() - 1));
        expectedMessage = expectedMessage.map(s -> s.replace("SER", ds.getClass().getName()));

        // When/Then
        if (expectedMessage.isPresent()) {
            Assertions.assertThatThrownBy(() -> new DatumTransformation(dd, mappers, ds))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage(expectedMessage.get());
        }
        else {
            Assertions.assertThatCode(() -> new DatumTransformation(dd, mappers, ds)).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldApply() throws IOException {
        // Given
        Datum<Integer> integerDatum = new Datum<>(NoSchema.INSTANCE, Integer.class, 1);
        DatumDeserializer<Integer> dd = mock(DatumDeserializer.class);
        Mockito.when(dd.returnedType()).thenReturn(Integer.class);
        Mockito.when(dd.deserialize(any(), any())).thenReturn(integerDatum);


        DatumMapper<Integer, Integer> dm = mock(DatumMapper.class);
        Mockito.when(dm.acceptedType()).thenReturn(Integer.class);
        Mockito.when(dm.returnedType()).thenReturn(Integer.class);
        Mockito.when(dm.transform(1)).thenReturn(2);

        DatumSerializer<Integer> ds = mock(DatumSerializer.class);
        Mockito.when(ds.acceptedType()).thenReturn(Integer.class);

        DatumTransformation dt = new DatumTransformation(dd, List.of(dm), ds);

        // When
        dt.apply(new Header[0], null, null);

        // Then
        verify(ds).serialize(eq(new Datum<>(NoSchema.INSTANCE, Integer.class, 2)), any());
    }
}