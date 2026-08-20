/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

/**
 * A {@link Runnable} that always throws a {@link FailException} with a fixed message.
 */
public class Fail implements Runnable {

    private final String message;

    /**
     * Creates a runnable.
     * @param message the message of the {@link FailException} thrown by {@link #run()}
     */
    public Fail(String message) {
        this.message = message;
    }

    @Override
    public void run() {
        throw new FailException(message);
    }
}
