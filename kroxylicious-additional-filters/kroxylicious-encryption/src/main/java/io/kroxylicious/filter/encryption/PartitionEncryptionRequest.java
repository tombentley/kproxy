/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.encryption;

import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.record.MemoryRecordsBuilder;

import java.util.stream.Stream;

public record PartitionEncryptionRequest(
        ProduceRequestData.PartitionProduceData ppd,
    MemoryRecordsBuilder builder,
    Stream<RecordEncryptionRequest> recordRequests) {
}
