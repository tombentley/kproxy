/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

/**
 * A property a caller can require of a composed {@link ContextPipeline}, verified once at construction time
 * rather than assumed. Deliberately consumed as a set of requirements at every call site that accepts one,
 * even though only one member exists today, so a future addition (e.g. a determinism requirement) doesn't
 * change any call site's type.
 */
public enum Requirement {

    /**
     * Requires that the pipeline's overall input type equal its overall output type - i.e. that composing
     * its stages end to end doesn't change the type of value being transformed, even if an individual stage
     * within it does (e.g. via an intermediate representation).
     */
    TYPE_PRESERVING
}
