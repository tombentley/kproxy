/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

/**
 * Abstraction over how KEKs are selected for records.
 */
public sealed interface KekSelector permits TopicNameBasedKekSelector, RecordBasedKekSelector {

}
