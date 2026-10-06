/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.util.List;
import java.util.function.Consumer;

import io.kroxylicious.kafka.common.record.internal.BaseRecords;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;

/**
 * One partition's records plus the manipulations applicable to it.
 *
 * @param topicName the topic name (already resolved and matched).
 * @param records the partition's records.
 * @param manipulations the manipulations applicable to this topic/direction, in priority order.
 * @param writeBack invoked with the transformed records.
 */
public record PartitionWorkItem(String topicName,
                                BaseRecords records,
                                List<CompiledManipulation<?, ?, ?>> manipulations,
                                Consumer<MemoryRecords> writeBack) {}
