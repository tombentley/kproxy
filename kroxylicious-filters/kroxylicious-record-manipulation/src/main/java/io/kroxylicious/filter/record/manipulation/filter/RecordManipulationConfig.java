/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import io.kroxylicious.filter.record.manipulation.kafka.RecordTransformConfig;

/**
 * Example:
 * <pre>{@code
 * topic: my-topic
 * direction: IN # for records being appended to my-topic, or OUT for records being read from my-topic
 * recordTransform:
 *   toRecordValue: ... # some transformation on the record value
 * }</pre>
 * @param topic The topic
 * @param direction The direction
 * @param recordTransform The transform
 */
public record RecordManipulationConfig(
        String topic,
        Direction direction,
        RecordTransformConfig recordTransform
) {
}
