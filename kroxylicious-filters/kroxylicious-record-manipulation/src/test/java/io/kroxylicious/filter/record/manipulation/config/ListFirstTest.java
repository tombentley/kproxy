/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.leangen.geantyref.TypeFactory;

import io.kroxylicious.filter.record.manipulation.op.PluginLookup;
import io.kroxylicious.filter.record.manipulation.op.OpContext;
import io.kroxylicious.filter.record.manipulation.op.TypeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ListFirstTest {

    @Mock
    PluginLookup lookup;

    OpContext opContext = new OpContext(new Random(), new byte[0]);

    @AfterEach
    void after() {
        Mockito.verifyNoInteractions(lookup);
    }

    @Test
    void setOfInteger() {
        var setOfInteger = TypeFactory.parameterizedClass(Set.class, Integer.class);
        Map<String, Object> configMap = Map.of();
        ListFirst<Integer> integerListFirst = new ListFirst<>();
        assertThatThrownBy(() -> integerListFirst.create(configMap, lookup, setOfInteger))
                .isInstanceOf(TypeException.class)
                .hasMessage("Argument type java.util.Set<java.lang.Integer> is not a subtype of interface java.util.List");
    }

    @Test
    void listOfInteger() {
        var listOfInteger = TypeFactory.parameterizedClass(List.class, Integer.class);
        var listFirst = new ListFirst<Integer>().create(Map.of(), lookup, listOfInteger);

        assertThat(listFirst.inputType()).isEqualTo(listOfInteger);
        assertThat(listFirst.outputType()).isEqualTo(Integer.class);
        assertThat(listFirst.apply(List.of(1), opContext)).isEqualTo(Integer.valueOf(1));
        assertThat(listFirst.apply(List.of(), opContext)).isNull();
    }

    @Test
    void arrayListOfInteger() {
        var listOfInteger = TypeFactory.parameterizedClass(ArrayList.class, Integer.class);
        var listFirst = new ListFirst<Integer>().create(Map.of(), lookup, listOfInteger);

        assertThat(listFirst.inputType()).isEqualTo(listOfInteger);
        assertThat(listFirst.outputType()).isEqualTo(Integer.class);
        assertThat(listFirst.apply(List.of(1), opContext)).isEqualTo(Integer.valueOf(1));
        assertThat(listFirst.apply(List.of(), opContext)).isNull();
    }

    @Test
    void listOfIntegerThrowIfEmpty() {
        var listOfInteger = TypeFactory.parameterizedClass(List.class, Integer.class);
        var listFirst = new ListFirst<Integer>().create(Map.of("throwIfEmpty", true), lookup, listOfInteger);

        assertThat(listFirst.inputType()).isEqualTo(listOfInteger);
        assertThat(listFirst.outputType()).isEqualTo(Integer.class);
        assertThat(listFirst.apply(List.of(1), opContext)).isEqualTo(Integer.valueOf(1));
        List<Integer> empty = List.of();
        assertThatThrownBy(() -> listFirst.apply(empty, opContext))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("No first element of an empty list");
    }

    @Test
    void listOfIntegerIfEmpty() {
        var listOfInteger = TypeFactory.parameterizedClass(List.class, Integer.class);
        var listFirst = new ListFirst<Integer>().create(Map.of("ifEmpty", 42), lookup, listOfInteger);

        assertThat(listFirst.inputType()).isEqualTo(listOfInteger);
        assertThat(listFirst.outputType()).isEqualTo(Integer.class);
        assertThat(listFirst.apply(List.of(1), opContext)).isEqualTo(Integer.valueOf(1));
        assertThat(listFirst.apply(List.of(), opContext)).isEqualTo(42);
    }

    @Test
    void listOfOptionalInteger() {
        var optionalInteger = TypeFactory.parameterizedClass(Optional.class, Integer.class);
        var listOfOptionalInteger = TypeFactory.parameterizedClass(List.class, optionalInteger);
        var listFirst = new ListFirst<Optional<Integer>>().create(Map.of(), lookup, listOfOptionalInteger);

        assertThat(listFirst.inputType()).isEqualTo(listOfOptionalInteger);
        assertThat(listFirst.outputType()).isEqualTo(optionalInteger);
        assertThat(listFirst.apply(List.of(Optional.of(1)), opContext)).isEqualTo(Optional.of(1));
        assertThat(listFirst.apply(List.of(), opContext)).isNull();
    }

    // <T> T first(List<T> list) {
    // // List<? extends Number> l = null;
    // // var x = first(l);
    // return list.isEmpty() ? null : list.get(0);
    // }

    @Test
    void listOfUnknown() {
        var listOfUnknown = TypeFactory.parameterizedClass(List.class, TypeFactory.unboundWildcard());
        var listFirst = new ListFirst<Integer>().create(Map.of(), lookup, listOfUnknown);

        assertThat(listFirst.inputType()).isEqualTo(listOfUnknown);
        assertThat(listFirst.outputType()).isEqualTo(TypeFactory.unboundWildcard());
        assertThat(listFirst.apply(List.of(1), opContext)).isEqualTo(Integer.valueOf(1));
        assertThat(listFirst.apply(List.of(), opContext)).isNull();
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void listOfUnknownIfEmpty() {
        var listOfUnknown = TypeFactory.parameterizedClass(List.class, TypeFactory.unboundWildcard());
        var listFirstWithDefault42 = new ListFirst<Integer>().create(Map.of("ifEmpty", 42), lookup, listOfUnknown);
        var listFirstWithDefaultFortyTwo = new ListFirst().create(Map.of("ifEmpty", "Forty Two"), lookup, listOfUnknown);

        assertThat(listFirstWithDefault42.inputType()).isEqualTo(listOfUnknown);
        assertThat(listFirstWithDefault42.outputType()).isEqualTo(TypeFactory.unboundWildcard());
        assertThat(listFirstWithDefault42.apply(List.of(1), opContext)).isEqualTo(Integer.valueOf(1));
        assertThat(listFirstWithDefault42.apply(List.of(), opContext)).isEqualTo(42);
        assertThat(listFirstWithDefaultFortyTwo.apply(List.of(1), opContext)).isEqualTo(1);
        assertThat(listFirstWithDefaultFortyTwo.apply(List.of(), opContext)).isInstanceOf(String.class);
    }

}
