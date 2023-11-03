/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import org.apache.kafka.common.record.Record;

public abstract non-sealed class RecordBasedKekSelector implements KekSelector {
    public abstract KeyRef keyRef(String topicName, Record kafkaRecord);
}
