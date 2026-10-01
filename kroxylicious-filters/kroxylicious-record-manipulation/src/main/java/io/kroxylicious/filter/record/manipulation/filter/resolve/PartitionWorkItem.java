/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.util.List;
import java.util.function.Consumer;

import io.kroxylicious.kafka.common.record.internal.MemoryRecords;

/**
 * One partition's records plus the manipulations applicable to it, handed to the
 * {@link ManipulationOrchestrator}. The orchestrator pools lookup ids across every work item in a
 * request before resolving, so that, for example, partitions sharing a schema/DEK are resolved together.
 *
 * @param topicName the topic name (already resolved and matched).
 * @param records the partition's records.
 * @param manipulations the manipulations applicable to this topic/direction, in priority order.
 * @param writeBack invoked with the transformed records; not called when nothing matched (the original
 *                  records are then left in place untouched).
 */
public record PartitionWorkItem(String topicName,
                                MemoryRecords records,
                                List<CompiledManipulation> manipulations,
                                Consumer<MemoryRecords> writeBack) {}
