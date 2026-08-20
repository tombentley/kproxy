/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

public class Fail implements Runnable {

    private final String message;

    public Fail(String message) {
        this.message = message;
    }

    @Override
    public void run() {
        throw new FailException(message);
    }
}
