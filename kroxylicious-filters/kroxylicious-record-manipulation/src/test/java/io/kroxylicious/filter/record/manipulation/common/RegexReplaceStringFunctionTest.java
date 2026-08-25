/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegexReplaceStringFunctionTest {

    @Test
    void replaceAll() {
        var rr = new RegexReplaceStringFunction("a*b", new RegexReplaceStringFunction.All("-"));
        assertThat(rr.apply("aabfooaabfooabfoob", null)).isEqualTo("-foo-foo-foo-");
    }

    @Test
    void replaceFirst() {
        var rr = new RegexReplaceStringFunction("dog", new RegexReplaceStringFunction.First("cat"));
        assertThat(rr.apply("zzzdogzzzdogzzz", null)).isEqualTo("zzzcatzzzdogzzz");
    }

    @Test
    void replaceAllOp() {
        var rr = new RegexReplaceStringFunction("dog", new RegexReplaceStringFunction.AllOp(new UppercaseStringFunction("en")));
        assertThat(rr.apply("zzzdogzzzdogzzz", null)).isEqualTo("zzzDOGzzzDOGzzz");
    }

    @Test
    void replaceFirstOp() {
        var rr = new RegexReplaceStringFunction("dog", new RegexReplaceStringFunction.FirstOp(new UppercaseStringFunction("en")));
        assertThat(rr.apply("zzzdogzzzdogzzz", null)).isEqualTo("zzzDOGzzzdogzzz");
    }
}