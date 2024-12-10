/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

import java.util.Locale;

import edu.umd.cs.findbugs.annotations.NonNull;

public class BeanPropertyStrategy implements PropertyStrategy {
    @Override
    public String accessorName(
                               String propertyName) {
        return prefix("get", propertyName);
    }

    @Override
    public String optionalAccessorName(
                                       String propertyName) {
        return prefix("opt", propertyName);
    }

    @Override
    public String mutatorName(
                              String propertyName) {
        return prefix("set", propertyName);
    }

    @NonNull
    private String prefix(String prefix, String propertyName) {
        int charsInFirstCodepoint = Character.charCount(propertyName.codePointAt(0));
        String methodName = CodeGen.quoteJavaKeyword(CodeGen.quoteNonIdentifierCharacters(
                prefix + propertyName.substring(0, charsInFirstCodepoint).toUpperCase(Locale.ROOT) + propertyName.substring(charsInFirstCodepoint)));
        if (methodName.equals("getClass")) {
            methodName += "_";
        }
        return methodName;
    }
}
