/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.util.function.BiFunction;

public class TryCatch<T, R> implements BiFunction<T, OpContext, R> {

    private final BiFunction<T, OpContext, R> try_;
    private final BiFunction<T, OpContext, R> catch_;

    public TryCatch(BiFunction<T, OpContext, R> catch_,
                    BiFunction<T, OpContext, R> try_) {
        this.catch_ = catch_;
        this.try_ = try_;
    }

    @Override
    public R apply(T t, OpContext opContext) {
        try {
            return try_.apply(t, opContext);
        }
        catch (Exception e) {
            return catch_.apply(t, opContext);
        }
    }
}
