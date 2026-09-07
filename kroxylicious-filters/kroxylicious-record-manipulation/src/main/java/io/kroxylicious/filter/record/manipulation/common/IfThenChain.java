/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import io.kroxylicious.filter.record.manipulation.op.OpContext;

public class IfThenChain<T, R> implements BiFunction<T, OpContext, R> {

    public record GuardedBranch<T, R>(BiPredicate<T, OpContext> guard, BiFunction<T, OpContext, R> op) {}

    private final List<GuardedBranch<T, R>> ifThens;
    private final BiFunction<T, OpContext, R> else_;


    public IfThenChain(List<GuardedBranch<T, R>> ifThens, BiFunction<T, OpContext, R> else_) {
        this.ifThens = ifThens;
        this.else_ = else_;
    }

    @Override
    public R apply(T t, OpContext opContext) {
        for (GuardedBranch<T, R> guardedBranch : ifThens) {
            if (guardedBranch.guard.test(t, opContext)) {
                return guardedBranch.op.apply(t, opContext);
            }
        }
        return else_.apply(t, opContext);
    }
}
