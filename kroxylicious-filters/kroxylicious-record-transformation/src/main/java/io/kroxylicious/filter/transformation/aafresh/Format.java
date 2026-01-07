/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A concrete data format, i.e. one that is sufficiently specified to be
 * read and written.
 * @param formatName The name of the format
 * @param encoding The format encoding
 * @param schema The schema. Or null if this format does not require a schema
 * @param <S>
 */
public record Format<S>(String formatName, String encoding, @Nullable S schema) {
}
