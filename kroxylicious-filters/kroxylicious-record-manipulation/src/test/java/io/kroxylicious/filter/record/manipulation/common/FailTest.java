/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FailTest {

    @Test
    void runThrowsFailExceptionWithConfiguredMessage() {
        // Given
        Fail fail = new Fail("boom");

        // When/Then
        assertThatThrownBy(fail::run)
                .isInstanceOf(FailException.class)
                .hasMessage("boom");
    }

}
