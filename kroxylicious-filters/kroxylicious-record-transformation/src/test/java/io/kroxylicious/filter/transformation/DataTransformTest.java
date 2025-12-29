/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import io.kroxylicious.filter.transformation.api.format.Deserializer;
import io.kroxylicious.filter.transformation.api.format.Serializer;
import io.kroxylicious.filter.transformation.api.mapper.Mapper;

import static org.mockito.Mockito.mock;

class DataTransformTest {

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
        var dd = mock(Deserializer.class);
        Mockito.when(dd.returnedType()).thenReturn(types.get(0));
        expectedMessage = expectedMessage.map(s -> s.replace("DESER", dd.getClass().getName()));

        List<Mapper<?, ?>> mappers = new ArrayList<>();
        for (int i = 1; i < types.size() - 1; i+=2) {
            var dm = mock(Mapper.class);
            Mockito.when(dm.acceptedType()).thenReturn(types.get(i));
            Mockito.when(dm.returnedType()).thenReturn(types.get(i+1));
            mappers.add(dm);
            String target = "MAPPER" + ((i - 1) / 2);
            expectedMessage = expectedMessage.map(s -> s.replace(target, dm.getClass().getName()));
        }

        var ds = mock(Serializer.class);
        Mockito.when(ds.acceptedType()).thenReturn(types.get(types.size() - 1));
        expectedMessage = expectedMessage.map(s -> s.replace("SER", ds.getClass().getName()));

        // When/Then
        if (expectedMessage.isPresent()) {
            Assertions.assertThatThrownBy(() -> new DataTransform(dd, mappers, ds))
                    .isExactlyInstanceOf(TypeException.class)
                    .hasMessage(expectedMessage.get());
        }
        else {
            Assertions.assertThatCode(() -> new DataTransform(dd, mappers, ds)).doesNotThrowAnyException();
        }
    }

}