/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.records;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * <p>Options for the asynchronous terminals ({@link RecordFrames#applyProduceAsync} /
 * {@link RecordFrames#applyFetchAsync}).</p>
 *
 * <p>The window size {@code W} bounds the number of records that have been admitted (had their transform
 * started) but not yet emitted. Because output is emitted in strict batch/offset order, this simultaneously
 * caps the number of transforms in flight at once <em>and</em> the number of decoded records held in memory
 * (~{@code W}), independently of how the client framed its batches. It is the proxy operator's knob for
 * concurrency, external-service load and memory.</p>
 *
 * <p>The {@code executor} is the single thread that owns the output assembly. All completions are marshalled
 * onto it, and the async terminals must be invoked on it. In the proxy this is the connection's
 * {@code FilterDispatchExecutor} (the Netty event loop); in tests it is typically a same-thread executor.</p>
 */
public final class TransformOptions {

    private final int windowSize;
    private final Executor executor;

    private TransformOptions(int windowSize, Executor executor) {
        this.windowSize = windowSize;
        this.executor = executor;
    }

    /**
     * Creates a new builder.
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@return the window size {@code W} (the maximum number of admitted-but-not-yet-emitted records)}
     */
    public int windowSize() {
        return windowSize;
    }

    /**
     * {@return the executor that owns the output assembly and onto which completions are marshalled}
     */
    public Executor executor() {
        return executor;
    }

    /**
     * A builder for {@link TransformOptions}.
     */
    public static final class Builder {

        private int windowSize = 1;
        private Executor executor;

        private Builder() {
        }

        /**
         * Sets the window size {@code W}: the maximum number of records that may be admitted (have their
         * transform started) but not yet emitted at once. Must be at least 1.
         * @param windowSize the window size
         * @return this builder
         */
        public Builder windowSize(int windowSize) {
            this.windowSize = windowSize;
            return this;
        }

        /**
         * Sets the executor that owns the output assembly and onto which transform completions are marshalled.
         * @param executor the assembly executor
         * @return this builder
         */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Builds the options.
         * @return the composed {@link TransformOptions}
         * @throws IllegalArgumentException if the window size is less than 1
         * @throws NullPointerException if no executor was set
         */
        public TransformOptions build() {
            if (windowSize < 1) {
                throw new IllegalArgumentException("windowSize must be at least 1, was " + windowSize);
            }
            Objects.requireNonNull(executor, "executor must be set");
            return new TransformOptions(windowSize, executor);
        }
    }
}
