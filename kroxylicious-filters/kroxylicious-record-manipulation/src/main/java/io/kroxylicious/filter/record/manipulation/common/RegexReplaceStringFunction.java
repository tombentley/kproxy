/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexReplaceStringFunction implements StringOp {

    public sealed interface Replacement{}

    /**
     * Replace all occurrences of the match with the given {@code replacement} string.
     * The replacement string can contain references to capturing groups defined in the pattern:
     * <dl>
     *     <dt><code>${<i>foo</i>}</code></dt><dd>The named capturing group with name <i>foo</i>.</dd>
     *     <dt><code>$<i>g</i></code></dt><dd>The <i>g</i>-th capturing group.</dd>
     * </dl>
     * @param replacement
     */
    public record All(String replacement)  implements Replacement{}

    /**
     * Replace the first all occurrence of the match with the given {@code replacement} string.
     * The replacement string can contain references to capturing groups defined in the pattern:
     * <dl>
     *     <dt><code>${<i>foo</i>}</code></dt><dd>The named capturing group with name <i>foo</i>.</dd>
     *     <dt><code>$<i>g</i></code></dt><dd>The <i>g</i>-th capturing group.</dd>
     * </dl>
     * @param replacement
     */
    public record First(String replacement) implements Replacement{}
    public record AllOp(StringOp groupOp) implements Replacement{}
    public record FirstOp(StringOp groupOp) implements Replacement{}

    private final Pattern pattern;
    private final Replacement replacement;

    public RegexReplaceStringFunction(String pattern,
                                      Replacement replacement) {
        this.pattern = Pattern.compile(pattern);
        this.replacement = replacement;
    }

    @Override
    public String apply(String s, Context context) {
        Matcher matcher = pattern.matcher(s);
        return switch (replacement) {
            case All(String string) -> matcher.replaceAll(string);
            case First(String string) -> matcher.replaceFirst(string);
            case AllOp(StringOp op) -> matcher.replaceAll(matchResult -> op.apply(matchResult.group(), context));
            case FirstOp(StringOp op) -> matcher.replaceFirst(matchResult -> op.apply(matchResult.group(), context));
        };
    }
}
