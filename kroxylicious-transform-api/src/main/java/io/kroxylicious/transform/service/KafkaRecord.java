/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.service;

import java.nio.ByteBuffer;
import java.util.List;

public interface KafkaRecord {
    long timestamp();

    ByteBuffer key();

    ByteBuffer value();

    List<? extends KafkaRecordHeader> headers();

}
