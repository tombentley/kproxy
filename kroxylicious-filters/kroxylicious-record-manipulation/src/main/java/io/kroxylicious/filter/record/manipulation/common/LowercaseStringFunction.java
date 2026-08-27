/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.Locale;
import java.util.function.BiFunction;

public class LowercaseStringFunction implements BiFunction<String, Context, String> {

    private final Locale locale;

    public LowercaseStringFunction(String languageTag) {
        this.locale = Locale.forLanguageTag(languageTag);
    }

    @Override
    public String apply(String s, Context context) {
        return s.toLowerCase(locale);
    }
}
