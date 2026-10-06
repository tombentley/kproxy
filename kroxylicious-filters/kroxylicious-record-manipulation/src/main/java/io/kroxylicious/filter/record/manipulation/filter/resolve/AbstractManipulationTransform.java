/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.util.List;

import io.kroxylicious.kafka.common.header.Header;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Shared matching logic for the per-record transforms driven by the {@code records} transform engine.
 *
 * <p>Topic and direction have already been matched when a {@link PartitionWorkItem} is built, so the only
 * per-record selection remaining is the synchronous header predicate: the first manipulation whose predicate
 * accepts the record's headers applies. (Schema-based selection is resolved asynchronously and is not yet
 * wired into this engine.)</p>
 */
abstract class AbstractManipulationTransform {

    private final List<CompiledManipulation<?, ?, ?>> manipulations;

    AbstractManipulationTransform(List<CompiledManipulation<?, ?, ?>> manipulations) {
        this.manipulations = manipulations;
    }

    /**
     * {@return the first manipulation whose header predicate accepts {@code headers}, or {@code null} if none}
     */
    @Nullable
    CompiledManipulation<?, ?, ?> firstMatch(Header[] headers) {
        for (CompiledManipulation<?, ?, ?> manipulation : manipulations) {
            var predicate = manipulation.headerPredicate();
            if (predicate == null || predicate.test(headers)) {
                return manipulation;
            }
        }
        return null;
    }
}
