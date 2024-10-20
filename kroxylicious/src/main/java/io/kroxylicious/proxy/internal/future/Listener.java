/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.kroxylicious.proxy.internal.future;

/**
 * Internal listener that signals success or failure when a future completes.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Listener<T> {

    /**
     * Signal the success.
     *
     * @param value the value
     */
    void onSuccess(T value);

    /**
     * Signal the failure
     *
     * @param failure the failure
     */
    void onFailure(Throwable failure);
}
