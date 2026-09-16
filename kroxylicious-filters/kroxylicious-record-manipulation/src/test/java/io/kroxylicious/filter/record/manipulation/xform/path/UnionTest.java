/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnionTest {
    @Test
    void singletonUnion() {
        var singleton = Union.of(String.class);
        assertThat(singleton.members()).isEqualTo(Set.of(String.class));
        assertThat(singleton).isEqualTo(Union.of(String.class));

        assertThat(singleton.isSupertypeOf(String.class)).isTrue();
        assertThat(singleton.isSupertypeOf(Integer.class)).isFalse();
        assertThat(singleton.isSupertypeOf(CharSequence.class)).isFalse();
        assertThat(singleton.isSupertypeOf(singleton)).isTrue();

        var charSeq = Union.of(CharSequence.class);
        assertThat(charSeq.isSupertypeOf(singleton)).isTrue();
        assertThat(singleton.isSupertypeOf(charSeq)).isFalse();

        var integer = Union.of(Integer.class);
        assertThat(integer.isSupertypeOf(singleton)).isFalse();
        assertThat(singleton.isSupertypeOf(integer)).isFalse();
    }

    @Test
    void subclassEliminatedByConstructor() {
        var union = Union.of(String.class, CharSequence.class);
        assertThat(Union.members(union)).isEqualTo(Set.of(CharSequence.class));
        var union2 = Union.of(CharSequence.class, String.class);
        assertThat(Union.members(union2)).isEqualTo(Set.of(CharSequence.class));
    }

    @Test
    void unionEliminatedByConstructor() {
        var union1 = Union.of(String.class);
        var union2 = Union.of(Union.of(String.class));
        var union3_1 = Union.of(Union.of(Union.of(String.class)));
        var union3_2 = Union.of(String.class, Union.of(String.class));
        var union3_3 = Union.of(Union.of(String.class), String.class);
        var union3_4 = Union.of(Union.of(Integer.class), String.class);
        var union3_5 = Union.of(Union.of(CharSequence.class), String.class);
        var union3_6 = Union.of(Union.of(String.class), CharSequence.class);
        var union3_7 = Union.of(Union.of(String.class), Union.of(CharSequence.class));
        var union3_8 = Union.of(Union.of(String.class), Union.of(Integer.class));
        assertThat(Union.members(union1)).isEqualTo(Set.of(String.class));
        assertThat(Union.members(union2)).isEqualTo(Set.of(String.class));
        assertThat(Union.members(union3_1)).isEqualTo(Set.of(String.class));
        assertThat(Union.members(union3_2)).isEqualTo(Set.of(String.class));
        assertThat(Union.members(union3_3)).isEqualTo(Set.of(String.class));
        assertThat(Union.members(union3_4)).isEqualTo(Set.of(Integer.class, String.class));
        assertThat(Union.members(union3_5)).isEqualTo(Set.of(CharSequence.class));
        assertThat(Union.members(union3_6)).isEqualTo(Set.of(CharSequence.class));
        assertThat(Union.members(union3_7)).isEqualTo(Set.of(CharSequence.class));
        assertThat(Union.members(union3_8)).isEqualTo(Set.of(String.class, Integer.class));
    }

    @Test
    void pairUnion() {
        var stringInteger = (Union) Union.of(String.class, Integer.class);
        assertThat(Union.members(stringInteger)).isEqualTo(Set.of(String.class, Integer.class));
        var integerString = (Union) Union.of(Integer.class, String.class);
        assertThat(Union.members(integerString)).isEqualTo(Set.of(Integer.class, String.class));
        assertThat(stringInteger).isEqualTo(integerString);
        assertThat(stringInteger).hasToString("class java.lang.String|class java.lang.Integer");
        assertThat(integerString).hasToString("class java.lang.Integer|class java.lang.String");
        assertThat(stringInteger.isSupertypeOf(String.class)).isTrue();
        assertThat(stringInteger.isSupertypeOf(Integer.class)).isTrue();
        assertThat(stringInteger.isSupertypeOf(Boolean.class)).isFalse();
        assertThat(stringInteger.isSupertypeOf(stringInteger)).isTrue();
        assertThat(stringInteger.isSupertypeOf(integerString)).isTrue();
        assertThat(integerString.isSupertypeOf(stringInteger)).isTrue();
        assertThat(integerString.isSupertypeOf(integerString)).isTrue();

        var integer = Union.of(Integer.class);
        var string = Union.of(Integer.class);
        assertThat(stringInteger.isSupertypeOf(string)).isTrue();
        assertThat(stringInteger.isSupertypeOf(integer)).isTrue();
        assertThat(integerString.isSupertypeOf(string)).isTrue();
        assertThat(integerString.isSupertypeOf(integer)).isTrue();
    }
}