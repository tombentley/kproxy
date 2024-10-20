/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.kroxylicious.proxy.internal.future;

import java.util.Objects;
import java.util.function.Function;

import io.kroxylicious.proxy.future.AsyncResult;
import io.kroxylicious.proxy.future.Future;
import io.kroxylicious.proxy.future.Handler;

/**
 * Succeeded future implementation.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public final class SucceededFuture<T> extends FutureBase<T> {

    /**
     * Stateless instance of empty results that can be shared safely.
     */
    public static final SucceededFuture EMPTY = new SucceededFuture(null);

    private final T result;

    /**
     * Create a future that has already succeeded
     * @param result the result
     */
    public SucceededFuture(T result) {
        this.result = result;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public Future<T> onSuccess(Handler<T> handler) {
        handler.handle(result);
        return this;
    }

    @Override
    public Future<T> onFailure(Handler<Throwable> handler) {
        return this;
    }

    @Override
    public Future<T> onComplete(Handler<AsyncResult<T>> handler) {
        if (handler instanceof Listener) {
            emitSuccess(result, (Listener<T>) handler);
        }
        else {
            handler.handle(this);
        }
        return this;
    }

    @Override
    public void addListener(Listener<T> listener) {
        emitSuccess(result, listener);
    }

    @Override
    public T result() {
        return result;
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public boolean succeeded() {
        return true;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public <V> Future<V> map(V value) {
        return new SucceededFuture<>(value);
    }

    @Override
    public Future<T> otherwise(Function<Throwable, T> mapper) {
        Objects.requireNonNull(mapper, "No null mapper accepted");
        return this;
    }

    @Override
    public Future<T> otherwise(T value) {
        return this;
    }

    @Override
    public String toString() {
        return "Future{result=" + result + "}";
    }
}
