/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption.coordinator;

import io.kroxylicious.filter.encryption.CoordinatedDekCache;

/**
 * Represents that data passed from the {@link DekCoordinator} to the {@link CoordinatedDekCache}.
 * This differs from a DEK obtained from a KMS because it encapsulates some policy about how the DEK can be used
 * @param notBefore The UNIX epoch before which this DEK should not be used.
 * @param notAfter The UNIX epoch after which this DEK should not be used.
 * @param kek The KEK
 * @param edek The wrapped DEK
 * @param <K> The type of the KEK reference
 * @param <E> The type of the wrapped DEK
 */
public record DekRecord<K, E>(long notBefore, long notAfter, K kek, E edek) {

}
